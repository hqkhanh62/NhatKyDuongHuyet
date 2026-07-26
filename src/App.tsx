
import { useEffect, useState, useMemo, useRef } from 'react'

type Rec = {
  id:string, ngay:string, buoi:string, gio:string, truoc?:number, sau?:number, thuoc?:string, lieu?:string, note?:string, timestamp:number
}

const LS_KEY='glucose_db_v2'

function loadDB():Rec[]{
  try{ const raw=localStorage.getItem(LS_KEY); if(raw) return JSON.parse(raw) }catch{}
  return []
}
function saveDB(d:Rec[]){ localStorage.setItem(LS_KEY, JSON.stringify(d)) }

export default function App(){
  const [data,setData]=useState<Rec[]>(()=>loadDB())
  const [form,setForm]=useState({ngay:new Date().toISOString().slice(0,10), buoi:'Sáng', gio:new Date().toTimeString().slice(0,5), truoc:'', sau:'', thuoc:'Mixtard', lieu:'', note:''})
  const canvasRef=useRef<HTMLCanvasElement>(null)
  const [filter,setFilter]=useState<'all'|'week'|'month'>('all')
  const [showGuide,setShowGuide]=useState(false)

  useEffect(()=>{ saveDB(data); draw() },[data, filter])
  
  const filtered = useMemo(()=>{
    if(filter==='all') return data
    const now=new Date()
    return data.filter(r=>{
      const d=new Date(r.ngay)
      if(filter==='week'){ const diff=(now.getTime()-d.getTime())/86400000; return diff<=7 }
      if(filter==='month'){ return d.getMonth()===now.getMonth() && d.getFullYear()===now.getFullYear() }
      return true
    })
  },[data,filter])

  const kpi = useMemo(()=>{
    const pre = data.map(d=>d.truoc).filter((x):x is number=>typeof x==='number')
    const post = data.map(d=>d.sau).filter((x):x is number=>typeof x==='number')
    const all = [...pre,...post]
    const inTarget = all.filter(v=>v>=3.9 && v<=10).length
    const pct = all.length? Math.round(inTarget/all.length*100):0
    const avgPre = pre.length? (pre.reduce((a,b)=>a+b,0)/pre.length).toFixed(1):'-'
    const avgPost = post.length? (post.reduce((a,b)=>a+b,0)/post.length).toFixed(1):'-'
    const high = all.filter(v=>v>11.1).length
    const low = all.filter(v=>v<3.9).length
    return {avgPre, avgPost, pct, high, low}
  },[data])

  function draw(){
    const c=canvasRef.current; if(!c) return
    const ctx=c.getContext('2d'); if(!ctx) return
    const W=c.width=800, H=c.height=300
    ctx.clearRect(0,0,W,H)
    // grid
    ctx.fillStyle='#f0f9ff'; ctx.fillRect(0,0,W,H)
    ctx.strokeStyle='#e2e8f0'; ctx.lineWidth=1
    for(let y=0;y<=5;y++){ ctx.beginPath(); ctx.moveTo(50,y*50+20); ctx.lineTo(W-20,y*50+20); ctx.stroke() }
    // target bands
    ctx.fillStyle='rgba(14,165,233,0.1)'; ctx.fillRect(50, 50, W-70, 100)
    // lines
    const sorted=[...filtered].sort((a,b)=> new Date(a.ngay+'T'+a.gio).getTime() - new Date(b.ngay+'T'+b.gio).getTime())
    const vals=sorted.map(r=>r.sau ?? r.truoc).filter((x):x is number=>typeof x==='number')
    if(vals.length<2) return
    const min=3, max=14
    const colorMap:any={Sáng:'#0ea5e9', Trưa:'#f59e0b', Chiều:'#8b5cf6', Tối:'#ec4899'}
    sorted.forEach((r,i)=>{
      const v=r.sau??r.truoc; if(v===undefined) return
      const x=50 + i/(sorted.length-1)*(W-70)
      const y=20 + (max-v)/(max-min)*250
      ctx.fillStyle=colorMap[r.buoi]||'#000'
      ctx.beginPath(); ctx.arc(x,y,5,0,Math.PI*2); ctx.fill()
    })
  }

  function add(){
    const rec:Rec={
      id: Date.now().toString(),
      ngay: form.ngay, buoi: form.buoi, gio: form.gio,
      truoc: form.truoc? parseFloat(form.truoc): undefined,
      sau: form.sau? parseFloat(form.sau): undefined,
      thuoc: form.thuoc, lieu: form.lieu, note: form.note,
      timestamp: Date.now()
    }
    setData([rec,...data])
    setForm({...form, truoc:'', sau:'', note:''})
  }

  function exportCSV(onlyWeek=false){
    let rows = onlyWeek? filtered : data
    const header='Ngày,Buổi,Loại insulin/thuốc,Liều (đv/viên),Giờ tiêm/uống,Đường huyết trước (mmol/L),Đường huyết sau 2 giờ (mmol/L),Triệu chứng/Ghi chú'
    const csv=[header, ...rows.map(r=>`${r.ngay},${r.buoi},${r.thuoc||''},${r.lieu||''},${r.gio},${r.truoc||''},${r.sau||''},${(r.note||'').replace(/,/g,' ')}`)].join('\n')
    const blob=new Blob(['\ufeff'+csv],{type:'text/csv;charset=utf-8'})
    const url=URL.createObjectURL(blob)
    const a=document.createElement('a'); a.href=url; a.download= onlyWeek? `duong-huyet-tuan-${new Date().toISOString().slice(0,10)}.csv` : `duong-huyet-${new Date().toISOString().slice(0,10)}.csv`; a.click()
  }

  function exportPDF(){
    // @ts-ignore
    const { jsPDF } = window.jspdf
    const doc=new jsPDF()
    doc.text('Bao cao Duong Huyet - NhatKyDuongHuyet',10,15)
    doc.text(`Ngay: ${new Date().toLocaleDateString('vi-VN')} - Tong: ${data.length} ban ghi`,10,25)
    doc.text(`Doi TB: ${kpi.avgPre} - Sau an TB: ${kpi.avgPost} - Trong muc tieu: ${kpi.pct}%`,10,35)
    doc.text(`Cao >11.1: ${kpi.high} - Thap <3.9: ${kpi.low}`,10,45)
    // table simple
    let y=55
    filtered.slice(0,30).forEach(r=>{
      doc.text(`${r.ngay} ${r.buoi} ${r.gio} ${r.truoc||''}/${r.sau||''} ${r.note||''}`.slice(0,100),10,y)
      y+=7; if(y>280){ doc.addPage(); y=15 }
    })
    doc.save(`bao-cao-tuan-${new Date().toISOString().slice(0,10)}.pdf`)
  }

  function handleImport(e:any){
    const file=e.target.files[0]; if(!file) return
    const reader=new FileReader()
    reader.onload=()=>{
      const text=reader.result as string
      const lines=text.split('\n').slice(1)
      const newRecs:Rec[]=lines.filter(l=>l.trim()).map((l,i)=>{
        const c=l.split(',')
        return { id: `imp-${Date.now()}-${i}`, ngay: c[0]?.trim()||new Date().toISOString().slice(0,10), buoi:c[1]||'Sáng', thuoc:c[2], lieu:c[3], gio:c[4]||'06:00', truoc:c[5]?parseFloat(c[5]):undefined, sau:c[6]?parseFloat(c[6]):undefined, note:c[7], timestamp:Date.now() }
      })
      setData([...newRecs, ...data])
    }
    reader.readAsText(file)
  }

  return (
    <div className="min-h-screen p-4 max-w-5xl mx-auto">
      <header className="flex justify-between items-center mb-4">
        <h1 className="text-2xl font-bold text-sky-600">💉 Nhật Ký Đường Huyết</h1>
        <button onClick={()=>setShowGuide(true)} className="bg-sky-600 text-white px-4 py-2 rounded-xl">📌 Ghim ra màn hình</button>
      </header>

      {!window.matchMedia('(display-mode: standalone)').matches && (
        <div className="bg-sky-50 border border-sky-200 p-3 rounded-xl mb-4">💡 Mẹo: Ghim dashboard này ra màn hình chờ để đo hàng ngày nhanh hơn <button onClick={()=>setShowGuide(true)} className="underline font-semibold">Xem hướng dẫn</button></div>
      )}

      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
        <div className="bg-white p-4 rounded-2xl shadow"><div className="text-sm text-gray-500">Đói TB</div><div className="text-xl font-bold">{kpi.avgPre} mmol/L</div></div>
        <div className="bg-white p-4 rounded-2xl shadow"><div className="text-sm text-gray-500">Sau ăn TB</div><div className="text-xl font-bold">{kpi.avgPost} mmol/L</div></div>
        <div className="bg-white p-4 rounded-2xl shadow"><div className="text-sm text-gray-500">Trong mục tiêu</div><div className="text-xl font-bold">{kpi.pct}%</div></div>
        <div className="bg-white p-4 rounded-2xl shadow"><div className="text-sm text-gray-500">Cao/Thấp</div><div className="text-xl font-bold">{kpi.high}/{kpi.low}</div></div>
      </div>

      <div className="bg-white p-4 rounded-2xl shadow mb-6">
        <h2 className="font-semibold mb-3">Nhập nhanh</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
          <input type="date" value={form.ngay} onChange={e=>setForm({...form,ngay:e.target.value})} className="border p-2 rounded-lg" />
          <select value={form.buoi} onChange={e=>setForm({...form,buoi:e.target.value})} className="border p-2 rounded-lg"><option>Sáng</option><option>Trưa</option><option>Chiều</option><option>Tối</option></select>
          <input type="time" value={form.gio} onChange={e=>setForm({...form,gio:e.target.value})} className="border p-2 rounded-lg" />
          <input placeholder="Thuốc" value={form.thuoc} onChange={e=>setForm({...form,thuoc:e.target.value})} className="border p-2 rounded-lg" />
          <input placeholder="Trước (mmol)" value={form.truoc} onChange={e=>setForm({...form,truoc:e.target.value})} className="border p-2 rounded-lg" />
          <input placeholder="Sau 2h (mmol)" value={form.sau} onChange={e=>setForm({...form,sau:e.target.value})} className="border p-2 rounded-lg" />
          <input placeholder="Liều" value={form.lieu} onChange={e=>setForm({...form,lieu:e.target.value})} className="border p-2 rounded-lg" />
          <input placeholder="Ghi chú" value={form.note} onChange={e=>setForm({...form,note:e.target.value})} className="border p-2 rounded-lg" />
        </div>
        <button onClick={add} className="mt-3 bg-sky-600 text-white px-6 py-2 rounded-xl">Lưu</button>
      </div>

      <div className="bg-white p-4 rounded-2xl shadow mb-6">
        <div className="flex justify-between items-center mb-2">
          <h2 className="font-semibold">Biểu đồ</h2>
          <select value={filter} onChange={e=>setFilter(e.target.value as any)} className="border p-1 rounded"><option value="all">Tất cả</option><option value="week">Tuần này</option><option value="month">Tháng này</option></select>
        </div>
        <canvas ref={canvasRef} width={800} height={300} className="w-full"></canvas>
      </div>

      <div className="bg-white p-4 rounded-2xl shadow mb-6">
        <div className="flex flex-wrap gap-2 mb-3">
          <button onClick={()=>exportCSV(false)} className="bg-green-600 text-white px-4 py-2 rounded-xl">📊 Xuất CSV - Tất cả</button>
          <button onClick={()=>exportCSV(true)} className="border border-green-600 text-green-600 px-4 py-2 rounded-xl">🗓️ Xuất CSV - Tuần này</button>
          <button onClick={exportPDF} className="bg-slate-800 text-white px-4 py-2 rounded-xl">📄 Xuất PDF tuần</button>
          <label className="border px-4 py-2 rounded-xl cursor-pointer">📥 Import CSV<input type="file" accept=".csv" onChange={handleImport} className="hidden" /></label>
        </div>
        <div className="overflow-auto max-h-96">
          <table className="w-full text-sm">
            <thead><tr className="text-left border-b"><th>Ngày</th><th>Buổi</th><th>Giờ</th><th>Trước</th><th>Sau</th><th>Thuốc</th><th>Ghi chú</th></tr></thead>
            <tbody>{filtered.map(r=><tr key={r.id} className="border-b"><td>{r.ngay}</td><td>{r.buoi}</td><td>{r.gio}</td><td className={r.truoc && r.truoc>11.1?'text-red-600 font-bold':''}>{r.truoc||''}</td><td className={r.sau && r.sau>11.1?'text-red-600 font-bold':''}>{r.sau||''}</td><td>{r.thuoc} {r.lieu}</td><td>{r.note}</td></tr>)}</tbody>
          </table>
        </div>
      </div>

      {showGuide && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50" onClick={()=>setShowGuide(false)}>
          <div className="bg-white rounded-2xl p-6 max-w-md w-full" onClick={e=>e.stopPropagation()}>
            <h3 className="font-bold text-lg mb-3">📌 Cách ghim ra màn hình chính</h3>
            <div className="space-y-3 text-sm">
              <div><b>iPhone (Safari):</b> Bấm nút Chia sẻ - Thêm vào MH chính - Thêm</div>
              <div><b>Android (Chrome):</b> Bấm ⋮ 3 chấm góc trên - Thêm vào Màn hình chính / Cài đặt ứng dụng</div>
              <div className="bg-yellow-50 p-2 rounded">Nếu đang mở trong app MetaAI/Facebook, hãy bấm "Mo trong trinh duyet" trước rồi mới ghim được.</div>
            </div>
            <button onClick={()=>setShowGuide(false)} className="mt-4 w-full bg-sky-600 text-white py-2 rounded-xl">Đã hiểu</button>
          </div>
        </div>
      )}

      <footer className="text-center text-xs text-gray-400 mt-8">Công cụ theo dõi cá nhân, không thay thế tư vấn y tế. Dữ liệu lưu offline trên thiết bị.</footer>
    </div>
  )
}
