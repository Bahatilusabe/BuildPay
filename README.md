# BuildPay

BuildPay is an offline-ready desktop application built with Electron.

## Build an installer

Install the project dependencies, then run the build command for the operating
system you are packaging on:

```bash
npm run dist:mac:x64
npm run dist:mac:arm64
```

The architecture-specific installers are written to `dist/`. Build for other
platforms with:

```bash
npm run dist:win
npm run dist:linux
```

- macOS: `.dmg`
- Windows: `.exe` installer
- Linux: `.AppImage`

For Windows and Linux, build on the target operating system:

```bash
npm run dist:win
npm run dist:linux
```

Tagged GitHub releases automatically build the Windows and Linux installers
on GitHub-hosted runners and attach them to the release. See the [BuildPay
releases](https://github.com/Bahatilusabe/BuildPay/releases) page to download
the installers.

For release builds that users can install without operating-system security
warnings, sign and notarize the macOS app and sign the Windows installer with
your publisher certificates.

## Run from source

```bash
npm install
npm start
```

BuildPay stores its local database in Electron's per-user application data
directory, so installing an update does not replace the user's database.
