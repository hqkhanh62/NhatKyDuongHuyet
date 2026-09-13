import GlucoseOcrForm from "@/components/glucose-ocr-form";

export const dynamic = "force-dynamic";

export default function HomePage() {
  return (
    <main className="min-h-screen bg-slate-100">
      <GlucoseOcrForm />
    </main>
  );
}
