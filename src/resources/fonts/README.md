Place the required TTF font files here so the LoginLauncher can load them at runtime and your UI renders pixel-perfect.

Launcher checks for these exact filenames (case-sensitive) in src/resources/fonts/:
- Montserrat-ExtraBold.ttf
- Montserrat-Medium.ttf
- Inter-Medium.ttf
- Inter-SemiBold.ttf
- Inter-Bold.ttf

Steps:
1. Copy the fonts into this folder: src/resources/fonts/
2. Run the LoginLauncher (or the project's run script). The launcher will print which fonts were loaded.

If you use different filenames, either rename the files to match the list above or edit src/ui/login/LoginLauncher.java to match your filenames.
