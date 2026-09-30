param(
    [string]$Validator = (Join-Path $PSScriptRoot '../.tools/glslang-16.6.0/bin/glslang.exe'),
    [string]$MinecraftJar = (Join-Path $PSScriptRoot '../.gradle-user-home/caches/fabric-loom/1.21.11/minecraft-client.jar')
)
$ErrorActionPreference = 'Stop'
$project = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$output = Join-Path $project '.tools/shader-validation'
if (!(Test-Path -LiteralPath $Validator)) { throw 'Pass -Validator with an installed Khronos glslang executable. This script never downloads tools.' }
if (!(Test-Path -LiteralPath $MinecraftJar)) { throw 'Pass -MinecraftJar pointing to the Minecraft 1.21.11 client JAR from Loom.' }
New-Item -ItemType Directory -Path $output -Force | Out-Null
$zip = [IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($MinecraftJar))
try {
    foreach ($shader in @('sky_dome','custom_stars','visual_grade','live_background','glow_extract')) {
        foreach ($stage in @(@('vsh','vert'),@('fsh','frag'))) {
            $source = Get-Content -LiteralPath (Join-Path $project "src/main/resources/assets/nexvisuals/shaders/core/$shader.$($stage[0])") -Raw
            $expanded = [regex]::Replace($source, '#moj_import <minecraft:([^>]+)>', [System.Text.RegularExpressions.MatchEvaluator]{
                param($match)
                $entry = $zip.GetEntry('assets/minecraft/shaders/include/'+$match.Groups[1].Value)
                if (!$entry) { throw "Missing vanilla shader import: $match" }
                $reader = [IO.StreamReader]::new($entry.Open())
                try { return [regex]::Replace($reader.ReadToEnd(), '(?m)^#version[^\r\n]*', '') }
                finally { $reader.Dispose() }
            })
            [IO.File]::WriteAllText((Join-Path $output "$shader.$($stage[1])"), $expanded, [Text.UTF8Encoding]::new($false))
        }
        $diagnostics = & $Validator -l -q (Join-Path $output "$shader.vert") (Join-Path $output "$shader.frag")
        if ($LASTEXITCODE -ne 0) { $diagnostics | Write-Output; throw "GLSL compile/link failed: $shader" }
        $blocks = @{ sky_dome = @('SkyConfig',176); custom_stars = @('StarConfig',48); visual_grade = @('VisualConfig',176); live_background = @('BackgroundConfig',96); glow_extract = @('HighlightConfig',16) }
        $block = $blocks[$shader]
        if (($diagnostics -join "`n") -notmatch "(?m)^$($block[0]):.*size $($block[1]),") {
            $diagnostics | Write-Output; throw "GPU uniform layout differs from Java buffer: $shader"
        }
        Write-Output "GLSL compile/link + uniform layout OK: $shader ($($block[1]) bytes)"
    }
} finally { $zip.Dispose() }
Write-Output 'Validated five GLSL 330 vertex/fragment pairs with actual Minecraft 1.21.11 imports. No game or GPU window was opened.'
