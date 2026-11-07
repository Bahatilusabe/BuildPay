'use strict';

const { app, BrowserWindow } = require('electron');
const path = require('path');
const { initializeDatabase } = require('./database/database');
const { registerBuildPayService } = require('./services/buildpay-service');

let databaseRuntime = null;

const createWindow = () => {
  const win = new BrowserWindow({
    width: 1440,
    height: 980,
    minWidth: 1180,
    minHeight: 760,
    backgroundColor: '#08151c',
    frame: false,
    titleBarStyle: 'hiddenInset',
    title: 'Builder',
    icon: path.join(__dirname, 'assets', 'icon.png'),
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
      preload: path.join(__dirname, 'preload.js')
    }
  });

  win.loadFile(path.join(__dirname, 'login.html'));

  win.once('ready-to-show', () => {
    win.show();
  });
};

app.whenReady().then(() => {
  databaseRuntime = initializeDatabase(app.getPath('userData'));
  registerBuildPayService(databaseRuntime.database, databaseRuntime.health, app.getPath('userData'));
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
}).catch((error) => {
  console.error(error);
  app.quit();
});

app.on('before-quit', () => {
  if (databaseRuntime) {
    databaseRuntime.close();
    databaseRuntime = null;
  }
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
