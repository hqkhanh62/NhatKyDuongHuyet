"use client";

import { inferAndNormalizeGlucose, mgDlToMmol } from "@/lib/glucose";
import { useEffect, useMemo, useRef, useState } from "react";

type Entry = {
  id: number;
  userId: number | null;
  valueMgDl: number;
  originalValue: number;
  originalUnit: "mg/dL" | "mmol/L";
  source: "manual" | "camera_ocr";
  ocrRawText: string | null;
  ocrConfidence: number | null;
  note: string | null;
  measuredAt: string;
};

type AppUser = {
  id: number;
  email: string;
  fullName: string;
  role: "user" | "admin";
};

type AdminUser = {
  id: number;
  email: string;
  fullName: string;
  role: "user" | "admin";
  createdAt: string;
  updatedAt: string;
};

type OcrAttempt = {
  text: string;
  confidence: number;
};

export default function GlucoseOcrForm() {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const workerRef = useRef<any>(null);

  const [cameraOn, setCameraOn] = useState(false);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");

  const [user, setUser] = useState<AppUser | null>(null);
  const [authMode, setAuthMode] = useState<"login" | "register">("login");
  const [authEmail, setAuthEmail] = useState("");
  const [authPassword, setAuthPassword] = useState("");
  const [authName, setAuthName] = useState("");

  const [entries, setEntries] = useState<Entry[]>([]);

  const [readingInput, setReadingInput] = useState("");
  const [note, setNote] = useState("");
  const [measuredAt, setMeasuredAt] = useState(() => toLocalDatetimeInputValue(new Date()));

  const [ocrRawText, setOcrRawText] = useState("");
  const [ocrConfidence, setOcrConfidence] = useState<number | null>(null);

  const [exportPassphrase, setExportPassphrase] = useState("");
  const [exportFormat, setExportFormat] = useState<"json" | "csv">("json");

  const [adminUsers, setAdminUsers] = useState<AdminUser[]>([]);

  const parsedPreview = useMemo(() => inferAndNormalizeGlucose(readingInput), [readingInput]);

  useEffect(() => {
    void bootstrapAuth();
    return () => {
      stopCamera();
      void terminateWorker();
    };
  }, []);

  async function bootstrapAuth() {
    const res = await fetch("/api/auth/me", { cache: "no-store" });
    if (!res.ok) return;

    const data = (await res.json()) as { user: AppUser | null };
    if (data.user) {
      setUser(data.user);
      await loadEntries();
      if (data.user.role === "admin") {
        await loadAdminUsers();
      }
    }
  }

  async function handleAuthSubmit() {
    setMessage("");
    setBusy(true);

    try {
      const endpoint = authMode === "login" ? "/api/auth/login" : "/api/auth/register";
      const payload =
        authMode === "login"
          ? { email: authEmail, password: authPassword }
          : { email: authEmail, password: authPassword, fullName: authName };

      const res = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      const data = (await res.json()) as { error?: string; user?: AppUser };
      if (!res.ok || !data.user) {
        setMessage(data.error ?? "Không thể xác thực tài khoản.");
        return;
      }

      setUser(data.user);
      setAuthPassword("");
      setMessage(authMode === "login" ? "Đăng nhập thành công." : "Đăng ký thành công.");
      await loadEntries();
      if (data.user.role === "admin") {
        await loadAdminUsers();
      }
    } catch {
      setMessage("Lỗi kết nối khi xác thực.");
    } finally {
      setBusy(false);
    }
  }

  async function logout() {
    setBusy(true);
    try {
      await fetch("/api/auth/logout", { method: "POST" });
    } finally {
      setUser(null);
      setEntries([]);
      setAdminUsers([]);
      setBusy(false);
      setMessage("Đã đăng xuất.");
      stopCamera();
    }
  }

  async function loadEntries() {
    const res = await fetch("/api/glucose", { cache: "no-store" });
    if (!res.ok) return;
    const data = (await res.json()) as { entries: Entry[] };
    setEntries(data.entries ?? []);
  }

  async function loadAdminUsers() {
    const res = await fetch("/api/admin/users", { cache: "no-store" });
    if (!res.ok) return;
    const data = (await res.json()) as { users: AdminUser[] };
    setAdminUsers(data.users ?? []);
  }

  async function startCamera() {
    setMessage("");
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: { ideal: "environment" },
          width: { ideal: 1280 },
          height: { ideal: 720 },
        },
        audio: false,
      });

      streamRef.current = stream;
      setCameraOn(true);

      const video = videoRef.current;
      if (video) {
        video.srcObject = stream;
        await video.play();
      }
    } catch {
      setMessage("Không mở được camera. Hãy cấp quyền camera rồi thử lại.");
    }
  }

  function stopCamera() {
    const stream = streamRef.current;
    if (stream) {
      for (const track of stream.getTracks()) track.stop();
      streamRef.current = null;
    }
    setCameraOn(false);
  }

  async function terminateWorker() {
    if (workerRef.current) {
      await workerRef.current.terminate();
      workerRef.current = null;
    }
  }

  async function getWorker() {
    if (workerRef.current) return workerRef.current;
    const { createWorker } = await import("tesseract.js");
    const worker = await createWorker("eng");
    await worker.setParameters({
      tessedit_char_whitelist: "0123456789.,mMgGdDlLoO/",
      classify_bln_numeric_mode: "1",
      preserve_interword_spaces: "1",
    });
    workerRef.current = worker;
    return worker;
  }

  async function scanFromCamera() {
    setMessage("");

    const video = videoRef.current;
    if (!video || !cameraOn) {
      setMessage("Camera chưa sẵn sàng.");
      return;
    }

    if (!video.videoWidth || !video.videoHeight) {
      setMessage("Không đọc được khung hình camera.");
      return;
    }

    setBusy(true);

    try {
      const frameCanvas = document.createElement("canvas");
      frameCanvas.width = video.videoWidth;
      frameCanvas.height = video.videoHeight;

      const frameCtx = frameCanvas.getContext("2d");
      if (!frameCtx) throw new Error("ctx");
      frameCtx.drawImage(video, 0, 0);

      const roi = extractRoi(frameCanvas);
      const processed = preprocessForOcr(roi, false);
      const processedInverted = preprocessForOcr(roi, true);

      const worker = await getWorker();

      const attempts: OcrAttempt[] = [];
      for (const variant of [roi, processed, processedInverted]) {
        const result = await worker.recognize(variant);
        attempts.push({
          text: String(result.data?.text ?? ""),
          confidence: Number(result.data?.confidence ?? 0),
        });
      }

      const best = selectBestAttempt(attempts);
      if (!best) {
        setMessage("Không nhận diện được số đo rõ ràng. Hãy đưa máy gần hơn và giữ yên tay.");
        return;
      }

      const parsed = inferAndNormalizeGlucose(best.text);
      if (!parsed) {
        setOcrRawText(best.text.trim());
        setOcrConfidence(best.confidence);
        setMessage("OCR có đọc được chữ nhưng chưa suy ra chỉ số hợp lệ. Bạn có thể nhập tay để lưu.");
        return;
      }

      const display = parsed.unit === "mmol/L" ? `${parsed.value.toFixed(1)} mmol/L` : `${Math.round(parsed.value)} mg/dL`;
      setReadingInput(display);
      setOcrRawText(best.text.trim());
      setOcrConfidence(best.confidence);
      setMessage(`Đã nhận diện: ${display} (độ tin cậy OCR ${best.confidence.toFixed(0)}%)`);
    } catch {
      setMessage("Lỗi OCR. Vui lòng thử lại.");
    } finally {
      setBusy(false);
    }
  }

  async function saveEntry() {
    setMessage("");
    const parsed = inferAndNormalizeGlucose(readingInput);
    if (!parsed) {
      setMessage("Giá trị không hợp lệ. Ví dụ: 118 mg/dL hoặc 6.5 mmol/L.");
      return;
    }

    setBusy(true);
    try {
      const res = await fetch("/api/glucose", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          input: readingInput,
          source: ocrRawText ? "camera_ocr" : "manual",
          measuredAt: new Date(measuredAt).toISOString(),
          note,
          ocrRawText: ocrRawText || undefined,
          ocrConfidence: ocrConfidence ?? undefined,
        }),
      });

      const data = (await res.json()) as { error?: string };
      if (!res.ok) {
        setMessage(data.error ?? "Không thể lưu dữ liệu.");
        return;
      }

      setMessage("Đã lưu chỉ số đường huyết thành công.");
      setReadingInput("");
      setNote("");
      setOcrRawText("");
      setOcrConfidence(null);
      setMeasuredAt(toLocalDatetimeInputValue(new Date()));
      await loadEntries();
    } catch {
      setMessage("Lỗi kết nối khi lưu dữ liệu.");
    } finally {
      setBusy(false);
    }
  }

  async function exportEncrypted() {
    setMessage("");
    if (exportPassphrase.length < 12) {
      setMessage("Passphrase export phải từ 12 ký tự.");
      return;
    }

    setBusy(true);
    try {
      const res = await fetch("/api/glucose/export", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ format: exportFormat, passphrase: exportPassphrase }),
      });

      const data = (await res.json()) as {
        error?: string;
        fileName?: string;
        package?: Record<string, unknown>;
      };

      if (!res.ok || !data.fileName || !data.package) {
        setMessage(data.error ?? "Không thể export dữ liệu.");
        return;
      }

      downloadJson(data.fileName, data.package);
      setExportPassphrase("");
      setMessage("Đã tải file export mã hóa. Hãy lưu passphrase ở nơi an toàn.");
    } catch {
      setMessage("Lỗi kết nối khi export.");
    } finally {
      setBusy(false);
    }
  }

  if (!user) {
    return (
      <div className="mx-auto w-full max-w-lg px-4 py-12">
        <div className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-200">
          <h1 className="text-2xl font-bold text-slate-900">NhatKyDuongHuyet Secure</h1>
          <p className="mt-1 text-sm text-slate-600">Đăng nhập để truy cập dữ liệu bệnh án cá nhân.</p>

          <div className="mt-4 flex gap-2">
            <button
              onClick={() => setAuthMode("login")}
              className={`rounded-lg px-3 py-2 text-sm font-medium ${
                authMode === "login" ? "bg-emerald-600 text-white" : "bg-slate-100 text-slate-700"
              }`}
            >
              Đăng nhập
            </button>
            <button
              onClick={() => setAuthMode("register")}
              className={`rounded-lg px-3 py-2 text-sm font-medium ${
                authMode === "register" ? "bg-emerald-600 text-white" : "bg-slate-100 text-slate-700"
              }`}
            >
              Đăng ký
            </button>
          </div>

          {authMode === "register" ? (
            <>
              <label className="mt-3 block text-sm font-medium text-slate-700">Họ và tên</label>
              <input
                value={authName}
                onChange={(e) => setAuthName(e.target.value)}
                className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
              />
            </>
          ) : null}

          <label className="mt-3 block text-sm font-medium text-slate-700">Email</label>
          <input
            type="email"
            value={authEmail}
            onChange={(e) => setAuthEmail(e.target.value)}
            className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
          />

          <label className="mt-3 block text-sm font-medium text-slate-700">Mật khẩu</label>
          <input
            type="password"
            value={authPassword}
            onChange={(e) => setAuthPassword(e.target.value)}
            className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
          />

          <button
            onClick={handleAuthSubmit}
            disabled={busy}
            className="mt-4 rounded-lg bg-emerald-600 px-4 py-2 font-medium text-white hover:bg-emerald-700 disabled:opacity-60"
          >
            {busy ? "Đang xử lý..." : authMode === "login" ? "Đăng nhập" : "Tạo tài khoản"}
          </button>

          {message ? <p className="mt-3 text-sm text-slate-700">{message}</p> : null}
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto w-full max-w-6xl px-4 py-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Nhật ký đường huyết + Camera OCR</h1>
          <p className="mt-1 text-slate-600">
            Xin chào <b>{user.fullName}</b> ({user.role})
          </p>
        </div>
        <button onClick={logout} className="rounded-lg bg-slate-200 px-4 py-2 font-medium text-slate-800 hover:bg-slate-300">
          Đăng xuất
        </button>
      </div>

      <p className="mt-2 text-slate-600">Dữ liệu được cô lập theo tài khoản, có audit log và export mã hóa.</p>

      <div className="mt-6 grid gap-6 lg:grid-cols-2">
        <section className="rounded-2xl bg-white p-4 shadow-sm ring-1 ring-slate-200">
          <h2 className="text-lg font-semibold">1) Quét từ camera</h2>

          <div className="relative mt-3 overflow-hidden rounded-xl bg-black">
            <video ref={videoRef} className="aspect-video w-full object-cover" muted playsInline />
            <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
              <div className="h-[34%] w-[74%] rounded-lg border-2 border-emerald-400/90 shadow-[0_0_0_9999px_rgba(0,0,0,0.25)]" />
            </div>
          </div>

          <div className="mt-3 flex flex-wrap gap-2">
            {!cameraOn ? (
              <button onClick={startCamera} className="rounded-lg bg-emerald-600 px-4 py-2 font-medium text-white hover:bg-emerald-700">
                Mở camera
              </button>
            ) : (
              <>
                <button
                  onClick={scanFromCamera}
                  disabled={busy}
                  className="rounded-lg bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700 disabled:opacity-60"
                >
                  {busy ? "Đang OCR..." : "Chụp & nhận diện"}
                </button>
                <button onClick={stopCamera} className="rounded-lg bg-slate-200 px-4 py-2 font-medium text-slate-800 hover:bg-slate-300">
                  Tắt camera
                </button>
              </>
            )}
          </div>
        </section>

        <section className="rounded-2xl bg-white p-4 shadow-sm ring-1 ring-slate-200">
          <h2 className="text-lg font-semibold">2) Xác nhận & lưu</h2>

          <label className="mt-3 block text-sm font-medium text-slate-700">Chỉ số đo</label>
          <input
            value={readingInput}
            onChange={(e) => setReadingInput(e.target.value)}
            placeholder="Ví dụ: 126 mg/dL hoặc 7.0 mmol/L"
            className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
          />

          <label className="mt-3 block text-sm font-medium text-slate-700">Thời gian đo</label>
          <input
            type="datetime-local"
            value={measuredAt}
            onChange={(e) => setMeasuredAt(e.target.value)}
            className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
          />

          <label className="mt-3 block text-sm font-medium text-slate-700">Ghi chú</label>
          <input
            value={note}
            onChange={(e) => setNote(e.target.value)}
            placeholder="Trước ăn sáng / sau vận động..."
            className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"
          />

          {parsedPreview ? (
            <p className="mt-3 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-800">
              Chuẩn hóa: <b>{parsedPreview.valueMgDl} mg/dL</b> (~{mgDlToMmol(parsedPreview.valueMgDl)} mmol/L)
            </p>
          ) : (
            <p className="mt-3 rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-800">Chưa có giá trị hợp lệ để lưu.</p>
          )}

          {ocrRawText ? (
            <div className="mt-3 rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-600">
              <p>
                OCR raw: <span className="font-mono">{ocrRawText}</span>
              </p>
              <p>Độ tin cậy: {ocrConfidence?.toFixed(0) ?? "-"}%</p>
            </div>
          ) : null}

          <button
            onClick={saveEntry}
            disabled={busy || !parsedPreview}
            className="mt-4 rounded-lg bg-emerald-600 px-4 py-2 font-medium text-white hover:bg-emerald-700 disabled:opacity-60"
          >
            {busy ? "Đang lưu..." : "Lưu nhật ký"}
          </button>
        </section>
      </div>

      <section className="mt-6 rounded-2xl bg-white p-4 shadow-sm ring-1 ring-slate-200">
        <h2 className="text-lg font-semibold">3) Export bệnh án mã hóa</h2>
        <p className="mt-1 text-sm text-slate-600">File export sẽ được mã hóa AES-256-GCM, chỉ giải mã được bằng passphrase bạn đặt.</p>

        <div className="mt-3 grid gap-3 md:grid-cols-3">
          <input
            type="password"
            value={exportPassphrase}
            onChange={(e) => setExportPassphrase(e.target.value)}
            placeholder="Passphrase tối thiểu 12 ký tự"
            className="rounded-lg border border-slate-300 px-3 py-2"
          />
          <select
            value={exportFormat}
            onChange={(e) => setExportFormat(e.target.value as "json" | "csv")}
            className="rounded-lg border border-slate-300 px-3 py-2"
          >
            <option value="json">JSON</option>
            <option value="csv">CSV</option>
          </select>
          <button
            onClick={exportEncrypted}
            disabled={busy}
            className="rounded-lg bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 disabled:opacity-60"
          >
            {busy ? "Đang export..." : "Export mã hóa"}
          </button>
        </div>
      </section>

      {user.role === "admin" ? (
        <section className="mt-6 rounded-2xl bg-white p-4 shadow-sm ring-1 ring-slate-200">
          <h2 className="text-lg font-semibold">4) Quản trị người dùng (RBAC: admin)</h2>
          <div className="mt-3 overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-600">
                  <th className="px-2 py-2">ID</th>
                  <th className="px-2 py-2">Họ tên</th>
                  <th className="px-2 py-2">Email</th>
                  <th className="px-2 py-2">Role</th>
                </tr>
              </thead>
              <tbody>
                {adminUsers.map((u) => (
                  <tr key={u.id} className="border-b border-slate-100">
                    <td className="px-2 py-2">{u.id}</td>
                    <td className="px-2 py-2">{u.fullName}</td>
                    <td className="px-2 py-2">{u.email}</td>
                    <td className="px-2 py-2">{u.role}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}

      <section className="mt-8 rounded-2xl bg-white p-4 shadow-sm ring-1 ring-slate-200">
        <h2 className="text-lg font-semibold">Lịch sử gần đây</h2>
        <div className="mt-3 overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-slate-600">
                <th className="px-2 py-2">Thời gian</th>
                <th className="px-2 py-2">Giá trị</th>
                <th className="px-2 py-2">Nguồn</th>
                <th className="px-2 py-2">Ghi chú</th>
              </tr>
            </thead>
            <tbody>
              {entries.map((entry) => (
                <tr key={entry.id} className="border-b border-slate-100">
                  <td className="px-2 py-2">{new Date(entry.measuredAt).toLocaleString("vi-VN")}</td>
                  <td className="px-2 py-2 font-semibold">
                    {entry.valueMgDl} mg/dL <span className="text-slate-500">(~{mgDlToMmol(entry.valueMgDl)} mmol/L)</span>
                  </td>
                  <td className="px-2 py-2">{entry.source === "camera_ocr" ? "Camera OCR" : "Nhập tay"}</td>
                  <td className="px-2 py-2">{entry.note || "-"}</td>
                </tr>
              ))}

              {entries.length === 0 ? (
                <tr>
                  <td className="px-2 py-5 text-slate-500" colSpan={4}>
                    Chưa có dữ liệu.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </section>

      {message ? <p className="mt-4 text-sm text-slate-700">{message}</p> : null}
    </div>
  );
}

function selectBestAttempt(attempts: OcrAttempt[]): OcrAttempt | null {
  let winner: { attempt: OcrAttempt; score: number } | null = null;

  for (const attempt of attempts) {
    const parsed = inferAndNormalizeGlucose(attempt.text);
    const base = parsed ? 100 + parsed.score * 8 : 0;
    const score = base + attempt.confidence;

    if (!winner || score > winner.score) {
      winner = { attempt, score };
    }
  }

  return winner?.attempt ?? null;
}

function extractRoi(source: HTMLCanvasElement): HTMLCanvasElement {
  const w = source.width;
  const h = source.height;

  const cropW = Math.floor(w * 0.74);
  const cropH = Math.floor(h * 0.34);
  const x = Math.floor((w - cropW) / 2);
  const y = Math.floor((h - cropH) / 2);

  const out = document.createElement("canvas");
  out.width = cropW;
  out.height = cropH;

  const ctx = out.getContext("2d");
  if (!ctx) return out;

  ctx.drawImage(source, x, y, cropW, cropH, 0, 0, cropW, cropH);
  return out;
}

function preprocessForOcr(source: HTMLCanvasElement, invert = false): HTMLCanvasElement {
  const scale = 2;
  const out = document.createElement("canvas");
  out.width = source.width * scale;
  out.height = source.height * scale;

  const ctx = out.getContext("2d", { willReadFrequently: true });
  if (!ctx) return source;

  ctx.imageSmoothingEnabled = true;
  ctx.drawImage(source, 0, 0, out.width, out.height);

  const image = ctx.getImageData(0, 0, out.width, out.height);
  const data = image.data;

  let mean = 0;
  for (let i = 0; i < data.length; i += 4) {
    const gray = 0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2];
    mean += gray;
    data[i] = gray;
    data[i + 1] = gray;
    data[i + 2] = gray;
  }

  mean = mean / (data.length / 4);
  const threshold = Math.max(85, Math.min(190, mean * 0.92));

  for (let i = 0; i < data.length; i += 4) {
    let value = data[i] > threshold ? 255 : 0;
    if (invert) value = 255 - value;
    data[i] = value;
    data[i + 1] = value;
    data[i + 2] = value;
    data[i + 3] = 255;
  }

  ctx.putImageData(image, 0, 0);
  return out;
}

function toLocalDatetimeInputValue(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, "0");

  const year = date.getFullYear();
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  const hours = pad(date.getHours());
  const minutes = pad(date.getMinutes());

  return `${year}-${month}-${day}T${hours}:${minutes}`;
}

function downloadJson(fileName: string, payload: unknown) {
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: "application/json;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = fileName;
  a.click();
  URL.revokeObjectURL(url);
}
