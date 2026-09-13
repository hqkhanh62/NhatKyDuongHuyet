import type { CapacitorConfig } from "@capacitor/cli";

const webAppUrl = process.env.WEB_APP_URL;

const config: CapacitorConfig = {
  appId: "com.nhatkyduonghuyet.app",
  appName: "NhatKyDuongHuyet",
  webDir: "www",
  server: webAppUrl
    ? {
        url: webAppUrl,
        cleartext: false,
      }
    : undefined,
  android: {
    allowMixedContent: false,
  },
};

export default config;