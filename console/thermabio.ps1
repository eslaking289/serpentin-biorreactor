#!/usr/bin/env pwsh
[CmdletBinding()]
param(
    [switch]$NoPrompt,
    [string]$CsvPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Read-Number([string]$Label, [double]$Default) {
    while ($true) {
        $raw = Read-Host ("{0} [{1}]" -f $Label, $Default)
        if ([string]::IsNullOrWhiteSpace($raw)) { return $Default }
        $normalized = $raw.Trim().Replace(',', '.')
        $number = 0.0
        if ([double]::TryParse($normalized, [Globalization.NumberStyles]::Float, [Globalization.CultureInfo]::InvariantCulture, [ref]$number)) { return $number }
        Write-Host '  Ingresa un número válido.' -ForegroundColor Yellow
    }
}

function Get-Value([string]$Name, [string]$Prompt, [double]$Default) {
    if ($NoPrompt) { return $Default }
    return Read-Number $Prompt $Default
}

function Get-Nusselt([double]$Re, [double]$Pr) {
    if ($Re -lt 2300) { return 3.66 }
    $gnAt = {
        param([double]$R, [double]$P)
        $f = [math]::Pow((0.79 * [math]::Log($R) - 1.64), -2)
        return ($f / 8) * ($R - 1000) * $P / (1 + 12.7 * [math]::Sqrt($f / 8) * ([math]::Pow($P, 2.0 / 3.0) - 1))
    }
    if ($Re -ge 3000) { return & $gnAt $Re $Pr }
    $weight = ($Re - 2300) / 700
    return 3.66 * (1 - $weight) + (& $gnAt 3000 $Pr) * $weight
}

function Get-Darcy([double]$Re) {
    if ($Re -lt 2300) { return 64 / $Re }
    return [math]::Pow((0.79 * [math]::Log($Re) - 1.64), -2)
}

function Get-Lmtd([double]$D1, [double]$D2) {
    if ([math]::Abs($D1 - $D2) -lt 1e-9 * [math]::Max($D1, $D2)) { return ($D1 + $D2) / 2 }
    return ($D2 - $D1) / [math]::Log($D2 / $D1)
}

function Show-Value([string]$Label, [double]$Value, [string]$Unit, [int]$Digits = 3) {
    $format = 'N' + $Digits
    Write-Host ('  {0,-38} {1,14} {2}' -f $Label, $Value.ToString($format, [Globalization.CultureInfo]::InvariantCulture), $Unit)
}

$defaults = [ordered]@{
    dh=460.0; substrateRate=50.0; heatFactor=1.0; agitation=1.2
    tBroth=30.0; tIn=20.0; tOut=25.0; cp=4180.0; rho=997.0; mu=0.89; kf=0.60; coolantFlow=0.25
    tubeD=25.4; wallT=1.65; wallK=16.0; pitch=50.8; tankD=1.0; clearance=25.0; hBroth=1200.0; fouling=0.0002
}
$labels = @{
    dh='Entalpía metabólica, kJ/mol base (O₂)'; substrateRate='Consumo molar de base, mol/h'
    heatFactor='Fracción del calor liberado (0–1)'; agitation='Potencia de agitación al caldo, kW'
    tBroth='Temperatura uniforme del caldo, °C'; tIn='Entrada del refrigerante, °C'; tOut='Salida objetivo del refrigerante, °C'
    cp='Calor específico del refrigerante, J/(kg·K)'; rho='Densidad del refrigerante, kg/m³'
    mu='Viscosidad dinámica, mPa·s'; kf='Conductividad del refrigerante, W/(m·K)'; coolantFlow='Caudal másico disponible, kg/s'
    tubeD='Diámetro exterior del tubo, mm'; wallT='Espesor de pared, mm'; wallK='Conductividad pared, W/(m·K)'
    pitch='Paso entre vueltas, mm'; tankD='Diámetro interno del tanque, m'; clearance='Holgura radial a la pared, mm'
    hBroth='Coeficiente externo caldo-tubo, W/(m²·K)'; fouling='Resistencia total de ensuciamiento exterior, m²·K/W'
}

Write-Host ''
Write-Host '  THERMABIO  /  CONSOLA' -ForegroundColor Cyan
Write-Host '  Dimensionamiento preliminar de serpentín — operación local, sin red' -ForegroundColor DarkGray
Write-Host '  Unidades SI indicadas en cada entrada. Enter conserva el valor sugerido.' -ForegroundColor DarkGray
Write-Host ''
$v = [ordered]@{}
foreach ($key in $defaults.Keys) { $v[$key] = Get-Value $key $labels[$key] $defaults[$key] }

$errors = [System.Collections.Generic.List[string]]::new()
if (-not ($v.tBroth -gt $v.tOut -and $v.tBroth -gt $v.tIn)) { $errors.Add('El caldo debe estar a mayor temperatura que ambas corrientes de refrigerante.') }
if (-not ($v.tOut -gt $v.tIn)) { $errors.Add('La salida del refrigerante debe ser mayor que su entrada.') }
if ($v.dh -le 0 -or $v.substrateRate -lt 0 -or $v.heatFactor -lt 0 -or $v.heatFactor -gt 1 -or $v.agitation -lt 0) { $errors.Add('Entalpía > 0, consumo y agitación >= 0, y fracción de calor entre 0 y 1.') }
if ($v.cp -le 0 -or $v.rho -le 0 -or $v.mu -le 0 -or $v.kf -le 0 -or $v.coolantFlow -lt 0) { $errors.Add('Propiedades del refrigerante deben ser positivas y su caudal no negativo.') }
if ($v.tubeD -le 0 -or $v.wallT -le 0 -or $v.wallK -le 0 -or $v.pitch -le 0 -or $v.tankD -le 0 -or $v.clearance -lt 0 -or $v.hBroth -le 0 -or $v.fouling -lt 0) { $errors.Add('Geometría, conductividad y coeficiente externo deben ser positivos; holgura y ensuciamiento no negativos.') }
$Do = $v.tubeD / 1000; $wall = $v.wallT / 1000; $Di = $Do - 2 * $wall
if ($Di -le 0) { $errors.Add('El espesor de pared deja un diámetro interno nulo o negativo.') }
if ($v.pitch / 1000 -lt $Do) { $errors.Add('El paso entre vueltas no puede ser menor que el diámetro exterior del tubo.') }
$Dh = $v.tankD - 2 * $v.clearance / 1000 - $Do
if ($Dh -le $Do) { $errors.Add('El diámetro de hélice debe superar el diámetro exterior del tubo.') }
if ($errors.Count -gt 0) {
    Write-Host "`n  ENTRADAS NO VÁLIDAS" -ForegroundColor Red
    foreach ($message in $errors) { Write-Host "  • $message" -ForegroundColor Red }
    exit 1
}

$qReaction = $v.dh * $v.substrateRate * $v.heatFactor / 3600
$qTotal = $qReaction + $v.agitation
if ($qTotal -le 0) { Write-Error 'La carga térmica total debe ser mayor que cero.'; exit 1 }
$lm = Get-Lmtd ($v.tBroth - $v.tOut) ($v.tBroth - $v.tIn)
$mReq = $qTotal * 1000 / ($v.cp * ($v.tOut - $v.tIn))
$viscosity = $v.mu / 1000
$velocity = $mReq / ($v.rho * [math]::PI * $Di * $Di / 4)
$Re = 4 * $mReq / ([math]::PI * $Di * $viscosity)
$Pr = $viscosity * $v.cp / $v.kf
$hStraight = (Get-Nusselt $Re $Pr) * $v.kf / $Di
$hi = if ($Re -ge 2300) { $hStraight * (1 + 3.5 * $Di / $Dh) } else { $hStraight }
$rIn = $Do / ($Di * $hi)
$rWall = $Do * [math]::Log($Do / $Di) / (2 * $v.wallK)
$U = 1 / ($rIn + $rWall + 1 / $v.hBroth + $v.fouling)
$area = $qTotal * 1000 / ($U * $lm)
$length = $area / ([math]::PI * $Do)
$p = $v.pitch / 1000
$turnLength = [math]::Sqrt([math]::Pow([math]::PI * $Dh, 2) + $p * $p)
$turns = $length / $turnLength
$height = $turns * $p
$pressure = (Get-Darcy $Re) * ($length / $Di) * $v.rho * $velocity * $velocity / 2
$ratio = if ($mReq -gt 0) { $v.coolantFlow / $mReq } else { [double]::PositiveInfinity }
$regime = if ($Re -ge 4000) { 'Turbulento' } elseif ($Re -ge 2300) { 'Transición' } else { 'Laminar' }
$correlationValid = $Re -ge 3000 -and $Re -le 5e6 -and $Pr -ge 0.5 -and $Pr -le 2000

Write-Host "`n  RESULTADOS" -ForegroundColor Cyan
Write-Host '  ──────────────────────────────────────────────────────────────────────' -ForegroundColor DarkGray
Show-Value 'Carga metabólica' $qReaction 'kW'
Show-Value 'Carga térmica total' $qTotal 'kW'
Show-Value 'LMTD' $lm 'K'
Show-Value 'Caudal másico requerido' $mReq 'kg/s'
Show-Value 'Caudal másico disponible' $v.coolantFlow 'kg/s'
Show-Value 'Velocidad interna estimada' $velocity 'm/s'
Show-Value 'Reynolds' $Re '—' 0
Show-Value 'Prandtl' $Pr '—'
Show-Value 'Coeficiente interno estimado' $hi 'W/(m²·K)'
Show-Value 'Coeficiente externo ingresado' $v.hBroth 'W/(m²·K)'
Show-Value 'Coeficiente global exterior' $U 'W/(m²·K)'
Show-Value 'Área exterior requerida' $area 'm²'
Show-Value 'Longitud de tubo estimada' $length 'm'
Show-Value 'Vueltas teóricas' $turns '—'
Show-Value 'Altura centro a centro' $height 'm'
Show-Value 'Caída de presión, tubo recto eq.' $pressure 'Pa'
Write-Host ("  {0,-38} {1}" -f 'Régimen estimado', $regime)
Write-Host ("  {0,-38} {1}" -f 'Correlación Gnielinski en rango', $(if ($correlationValid) { 'Sí' } else { 'No; interpretar con cautela' }))
Write-Host ("  {0,-38} {1}" -f 'Capacidad de caudal', $(if ($ratio -ge 1) { 'Suficiente para el caudal objetivo' } else { 'Insuficiente para el caudal objetivo' })) -ForegroundColor $(if ($ratio -ge 1) { 'Green' } else { 'Yellow' })
Write-Host "`n  Alcance: estimación educativa preliminar. h externo es un dato de entrada; la caída" -ForegroundColor DarkGray
Write-Host '  de presión usa tubo recto equivalente y no incluye accesorios ni curvatura.' -ForegroundColor DarkGray
Write-Host '  Valida propiedades y resultados con bibliografía y un especialista antes de fabricar.' -ForegroundColor DarkGray

if ([string]::IsNullOrWhiteSpace($CsvPath)) { $CsvPath = Join-Path $PSScriptRoot 'resultado-thermabio.csv' }
$rows = @(
    [pscustomobject]@{Parametro='Carga metabólica'; Valor=$qReaction; Unidad='kW'},
    [pscustomobject]@{Parametro='Carga total'; Valor=$qTotal; Unidad='kW'},
    [pscustomobject]@{Parametro='LMTD'; Valor=$lm; Unidad='K'},
    [pscustomobject]@{Parametro='Caudal requerido'; Valor=$mReq; Unidad='kg/s'},
    [pscustomobject]@{Parametro='Velocidad'; Valor=$velocity; Unidad='m/s'},
    [pscustomobject]@{Parametro='Reynolds'; Valor=$Re; Unidad='—'},
    [pscustomobject]@{Parametro='Prandtl'; Valor=$Pr; Unidad='—'},
    [pscustomobject]@{Parametro='h interno'; Valor=$hi; Unidad='W/(m²·K)'},
    [pscustomobject]@{Parametro='U global exterior'; Valor=$U; Unidad='W/(m²·K)'},
    [pscustomobject]@{Parametro='Área requerida'; Valor=$area; Unidad='m²'},
    [pscustomobject]@{Parametro='Longitud'; Valor=$length; Unidad='m'},
    [pscustomobject]@{Parametro='Vueltas teóricas'; Valor=$turns; Unidad='—'},
    [pscustomobject]@{Parametro='Altura centro a centro'; Valor=$height; Unidad='m'},
    [pscustomobject]@{Parametro='Caída de presión'; Valor=$pressure; Unidad='Pa'},
    [pscustomobject]@{Parametro='Caudal disponible suficiente'; Valor=($ratio -ge 1); Unidad='booleano'}
)
$rows | Export-Csv -LiteralPath $CsvPath -NoTypeInformation -Encoding utf8
Write-Host "`n  Informe CSV guardado en: $([IO.Path]::GetFullPath($CsvPath))" -ForegroundColor Green
