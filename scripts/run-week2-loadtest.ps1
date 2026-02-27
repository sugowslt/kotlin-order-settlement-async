param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$OutFile = "c:\backendgo\project2\performance-week2-result.json"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-P95 {
    param([double[]]$Values)
    if ($Values.Count -eq 0) { return 0.0 }
    $sorted = $Values | Sort-Object
    $idx = [math]::Ceiling($sorted.Count * 0.95) - 1
    if ($idx -lt 0) { $idx = 0 }
    return [math]::Round([double]$sorted[$idx], 2)
}

function Invoke-Scenario {
    param(
        [string]$Name,
        [int]$Seconds,
        [int]$Concurrency,
        [double]$FailRatio,
        [int]$TargetRps
    )

    $totalRequests = $Seconds * $TargetRps
    if ($totalRequests -lt $Concurrency) {
        $totalRequests = $Concurrency
    }

    $basePerWorker = [math]::Floor($totalRequests / $Concurrency)
    $remainder = $totalRequests % $Concurrency
    $jobs = @()

    for ($worker = 1; $worker -le $Concurrency; $worker++) {
        $requestsForWorker = $basePerWorker
        if ($worker -le $remainder) {
            $requestsForWorker += 1
        }

        $jobs += Start-Job -ScriptBlock {
            param($RequestsForWorker, $FailRatio, $BaseUrl, $Worker, $TargetRps, $Concurrency)

            $rows = New-Object System.Collections.Generic.List[object]
            $workerRps = [math]::Max(1.0, [double]$TargetRps / [double]$Concurrency)
            $targetIntervalMs = [math]::Max(1.0, 1000.0 / $workerRps)
            $nextTick = [DateTime]::UtcNow

            for ($i = 0; $i -lt $RequestsForWorker; $i++) {
                $now = [DateTime]::UtcNow
                if ($now -lt $nextTick) {
                    Start-Sleep -Milliseconds ([int][math]::Max(1, ($nextTick - $now).TotalMilliseconds))
                }

                $orderId = [int64](([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() * 10) + $Worker)
                $forceFail = ((Get-Random -Minimum 0.0 -Maximum 1.0) -lt $FailRatio)
                $traceId = "trace-week2-{0}-{1}" -f $Worker, ([guid]::NewGuid().ToString("N").Substring(0, 8))

                $payloadObj = @{
                    orderId = $orderId
                    userId = 2000 + $Worker
                    amount = 12345.67
                    currency = "KRW"
                    traceId = $traceId
                    forceFail = $forceFail
                }

                $body = $payloadObj | ConvertTo-Json -Compress
                $sw = [System.Diagnostics.Stopwatch]::StartNew()
                $isError = $false
                $statusCode = 202

                try {
                    Invoke-RestMethod -Uri "$BaseUrl/api/v1/events/orders" -Method Post -ContentType "application/json" -Body $body -TimeoutSec 15 | Out-Null
                }
                catch {
                    $isError = $true
                    $statusCode = 500
                }
                finally {
                    $sw.Stop()
                }

                $rows.Add([pscustomobject]@{
                    ms = [double]$sw.Elapsed.TotalMilliseconds
                    error = [bool]$isError
                    statusCode = [int]$statusCode
                }) | Out-Null

                $nextTick = $nextTick.AddMilliseconds($targetIntervalMs)
            }

            return $rows
        } -ArgumentList $requestsForWorker, $FailRatio, $BaseUrl, $worker, $TargetRps, $Concurrency
    }

    Wait-Job -Job $jobs | Out-Null
    $allRows = @($jobs | Receive-Job)
    $jobs | Remove-Job -Force | Out-Null

    $latencies = @($allRows | ForEach-Object { [double]$_.ms })
    $total = $allRows.Count
    $errors = @($allRows | Where-Object { $_.error }).Count
    $errorRate = if ($total -eq 0) { 0 } else { [math]::Round(($errors / $total) * 100, 2) }
    $avg = if ($total -eq 0) { 0 } else { [math]::Round((($latencies | Measure-Object -Average).Average), 2) }
    $p95 = Get-P95 -Values $latencies
    $tps = [math]::Round(($total / $Seconds), 2)

    return [pscustomobject]@{
        scenario = $Name
        seconds = $Seconds
        concurrency = $Concurrency
        targetRps = $TargetRps
        failRatio = $FailRatio
        requests = $total
        errors = $errors
        errorRate = $errorRate
        avgMs = $avg
        p95Ms = $p95
        tps = $tps
    }
}

Write-Host "[Warmup] 60s, concurrency=10"
Invoke-Scenario -Name "Warmup" -Seconds 30 -Concurrency 10 -FailRatio 0.0 -TargetRps 100 | Out-Null
Start-Sleep -Seconds 10

$results = @()
Write-Host "[Scenario A] 60s, concurrency=10"
$results += Invoke-Scenario -Name "A" -Seconds 60 -Concurrency 10 -FailRatio 0.0 -TargetRps 120
Start-Sleep -Seconds 15
Write-Host "[Scenario B] 180s, concurrency=50"
$results += Invoke-Scenario -Name "B" -Seconds 180 -Concurrency 50 -FailRatio 0.0 -TargetRps 300
Start-Sleep -Seconds 20
Write-Host "[Scenario C] 180s, concurrency=30, failRatio=0.05"
$results += Invoke-Scenario -Name "C" -Seconds 180 -Concurrency 30 -FailRatio 0.05 -TargetRps 200

$results | ConvertTo-Json -Depth 4 | Set-Content -Path $OutFile -Encoding UTF8
$results | Format-Table scenario,seconds,concurrency,requests,errors,errorRate,avgMs,p95Ms,tps -AutoSize | Out-String | Write-Host
Write-Host "Saved: $OutFile"
