import { randomBytes, createCipheriv, scryptSync } from "node:crypto";

export type ExportRow = {
  id: number;
  valueMgDl: number;
  originalValue: number;
  originalUnit: "mg/dL" | "mmol/L";
  source: "manual" | "camera_ocr";
  ocrRawText: string | null;
  ocrConfidence: number | null;
  note: string | null;
  measuredAt: Date;
  createdAt: Date;
};

export function toCsv(rows: ExportRow[]): string {
  const headers = [
    "id",
    "valueMgDl",
    "originalValue",
    "originalUnit",
    "source",
    "ocrConfidence",
    "note",
    "measuredAt",
    "createdAt",
  ];

  const lines = [headers.join(",")];

  for (const row of rows) {
    const values = [
      row.id,
      row.valueMgDl,
      row.originalValue,
      row.originalUnit,
      row.source,
      row.ocrConfidence ?? "",
      escapeCsv(row.note ?? ""),
      row.measuredAt.toISOString(),
      row.createdAt.toISOString(),
    ];

    lines.push(values.join(","));
  }

  return lines.join("\n");
}

function escapeCsv(value: string): string {
  if (/[",\n]/.test(value)) {
    return `"${value.replaceAll('"', '""')}"`;
  }

  return value;
}

export function encryptPayload(plainText: string, passphrase: string) {
  const salt = randomBytes(16);
  const iv = randomBytes(12);
  const key = scryptSync(passphrase, salt, 32);

  const cipher = createCipheriv("aes-256-gcm", key, iv);
  const encrypted = Buffer.concat([cipher.update(plainText, "utf8"), cipher.final()]);
  const tag = cipher.getAuthTag();

  return {
    alg: "aes-256-gcm",
    kdf: "scrypt",
    salt: salt.toString("base64"),
    iv: iv.toString("base64"),
    tag: tag.toString("base64"),
    data: encrypted.toString("base64"),
  };
}
