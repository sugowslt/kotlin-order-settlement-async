param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$OutFile = "c:\backendgo\project2\performance-week2-kafka-ack-partition.json",
    [string]$Label = "default",
    [int]$Seconds = 60,
    [int]$Concurrency = 100,
    [int]$TargetRps = 900,
    [double]$FailRatio = 0.0
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

function Invoke-Load {
    param(
        [int]$Seconds,
        [int]$Concurrency,
        [int]$TargetRps,
        [double]$FailRatio
    )

    $totalRequests = $Seconds * $TargetRps
    $basePerWorker = [math]::Floor($totalRequests / $Concurrency)
    $remainder = $totalRequests % $Concurrency
    $jobs = @()

    for ($worker = 1; $worker -le $Concurrency; $worker++) {
        $requestsForWorker = $basePerWorker
        if ($worker -le $remainder) { $requestsForWorker += 1 }

        $jobs += Start-Job -ScriptBlock {
            param($RequestsForWorker, $FailRatio, $BaseUrl, $Worker, $TargetRps, $Concurrency, $Label)
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
                $traceId = "trace-kafka-compare-{0}-{1}-{2}" -f $Label, $Worker, ([guid]::NewGuid().ToString("N").Substring(0, 8))
                $forceFail = ((Get-Random -Minimum 0.0 -Maximum 1.0) -lt $FailRatio)

                $bodyObj = @{
                    orderId = $orderId
                    userId = 5000 + $Worker
                    amount = 45678.9
                    currency = "KRW"
                    traceId = $traceId
                    forceFail = $forceFail
                }
                $body = $bodyObj | ConvertTo-Json -Compress

                $sw = [System.Diagnostics.Stopwatch]::StartNew()
                $isError = $false
                try {
                    Invoke-RestMethod -Uri "$BaseUrl/api/v1/events/orders" -Method Post -ContentType "application/json" -Body $body -TimeoutSec 20 | Out-Null
                }
                catch {
                    $isError = $true
                }
                finally {
                    $sw.Stop()
                }

                $rows.Add([pscustomobject]@{
                    ms = [double]$sw.Elapsed.TotalMilliseconds
                    error = [bool]$isError
                }) | Out-Null

                $nextTick = $nextTick.AddMilliseconds($targetIntervalMs)
            }

            return $rows
        } -ArgumentList $requestsForWorker, $FailRatio, $BaseUrl, $worker, $TargetRps, $Concurrency, $Label
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
        label = $Label
        seconds = $Seconds
        concurrency = $Concurrency
        targetRps = $TargetRps
        requests = $total
        errors = $errors
        errorRate = $errorRate
        avgMs = $avg
        p95Ms = $p95
        tps = $tps
    }
}

Write-Host "[Run] label=$Label sec=$Seconds c=$Concurrency rps=$TargetRps"
$result = Invoke-Load -Seconds $Seconds -Concurrency $Concurrency -TargetRps $TargetRps -FailRatio $FailRatio

$result | ConvertTo-Json -Depth 4 | Set-Content -Path $OutFile -Encoding UTF8
$result | Format-Table label,seconds,concurrency,targetRps,requests,errors,errorRate,avgMs,p95Ms,tps -AutoSize | Out-String | Write-Host
Write-Host "Saved: $OutFile"