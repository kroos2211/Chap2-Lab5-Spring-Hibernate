Add-Type -AssemblyName System.Drawing

$project = Split-Path -Parent $PSScriptRoot
$logs = Join-Path $project 'resultats'
$images = Join-Path $project 'captures'
New-Item -ItemType Directory -Force -Path $images | Out-Null

function Read-Lines([string]$name) {
    $path = Join-Path $logs $name
    if (-not (Test-Path -LiteralPath $path)) { throw "Journal absent : $path" }
    return ,([string[]](Get-Content -LiteralPath $path -Encoding UTF8))
}

function Slice-Lines([string[]]$lines, [string]$first, [string]$last) {
    $start = -1
    $end = -1
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($start -lt 0 -and $lines[$i].Contains($first)) { $start = $i }
        if ($start -ge 0 -and $lines[$i].Contains($last)) { $end = $i; break }
    }
    if ($start -lt 0 -or $end -lt $start) { throw "Marqueurs absents : $first / $last" }
    return ,([string[]]$lines[$start..$end])
}

function Save-Excerpt([string]$filename, [string]$source, [string[]]$lines) {
    $font = [System.Drawing.Font]::new('Consolas', 24, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
    $small = [System.Drawing.Font]::new('Consolas', 17, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
    $measure = [System.Drawing.Bitmap]::new(1, 1)
    $graphics = [System.Drawing.Graphics]::FromImage($measure)
    $maxWidth = 0
    foreach ($line in $lines) {
        $width = $graphics.MeasureString($line, $font).Width
        if ($width -gt $maxWidth) { $maxWidth = $width }
    }
    $graphics.Dispose()
    $measure.Dispose()

    $canvasWidth = [Math]::Max(1200, [int][Math]::Ceiling($maxWidth + 120))
    $canvasHeight = [Math]::Max(350, 150 + 36 * $lines.Count)
    $bitmap = [System.Drawing.Bitmap]::new($canvasWidth, $canvasHeight)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.Clear([System.Drawing.Color]::FromArgb(248, 250, 252))
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $headingBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(33, 73, 112))
    $textBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(20, 31, 42))
    $rulePen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(190, 205, 220), 2)
    $graphics.DrawString('EXTRAIT DE JOURNAL REEL', $small, $headingBrush, 50, 24)
    $graphics.DrawString("Source : resultats/$source", $small, $headingBrush, 50, 52)
    $graphics.DrawLine($rulePen, 50, 87, $canvasWidth - 50, 87)
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $graphics.DrawString($lines[$i], $font, $textBrush, 50, 108 + 36 * $i)
    }
    $bitmap.Save((Join-Path $images $filename), [System.Drawing.Imaging.ImageFormat]::Png)
    $rulePen.Dispose(); $headingBrush.Dispose(); $textBrush.Dispose()
    $graphics.Dispose(); $bitmap.Dispose(); $font.Dispose(); $small.Dispose()
}

$compile = Read-Lines '01-compilation.txt'
$tests = Read-Lines '02-tests-junit.txt'
$package = Read-Lines '03-package.txt'
$hibernate = Read-Lines '04-test-hibernate.txt'
$main = Read-Lines '05-main-app.txt'

Save-Excerpt '01-compilation.png' '01-compilation.txt' (Slice-Lines $compile '--- compiler:' 'BUILD SUCCESS')
Save-Excerpt '02-spring-hibernate.png' '04-test-hibernate.txt' (Slice-Lines $hibernate '=== TEST INITIALISATION SPRING ===' '=== FIN DU TEST ===')
Save-Excerpt '03-creation-produits.png' '05-main-app.txt' (Slice-Lines $main '=== CREATION DES PRODUITS ===' '=== LISTE DES PRODUITS ===')
Save-Excerpt '04-liste-produits.png' '05-main-app.txt' (Slice-Lines $main '=== LISTE DES PRODUITS ===' 'Total en base :')
Save-Excerpt '05-update-delete.png' '05-main-app.txt' (Slice-Lines $main '=== MODIFICATION PRODUIT ===' 'Produit supprime, recherche en base :')
Save-Excerpt '06-validation-transactions.png' '05-main-app.txt' (Slice-Lines $main '=== VALIDATION PRIX NEGATIF ===' 'Verification en base apres rollback :')
Save-Excerpt '07-tests-junit.png' '02-tests-junit.txt' (Slice-Lines $tests '[INFO] Results:' 'BUILD SUCCESS')
Save-Excerpt '08-build-success.png' '03-package.txt' (Slice-Lines $package '--- jar:' 'BUILD SUCCESS')

if ((Get-ChildItem -LiteralPath $images -Filter '*.png').Count -ne 8) { throw 'Huit images attendues.' }
Write-Output '8 images de journaux reels creees dans captures/.'
