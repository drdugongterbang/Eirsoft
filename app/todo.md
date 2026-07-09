# TODO.md — Proyek Eirsoft (UAS Pemrograman Perangkat Bergerak)

> **record pengerjaan eirsoft:**
> Ini adalah rencana kerja untuk aplikasi Android **Eirsoft** — manajemen persewaan properti airsoft.
> Kerjakan **fase per fase, urut dari atas ke bawah**. Jangan lompat ke fase berikutnya sebelum fase sebelumnya selesai dan bisa di-build tanpa error.
> Setiap kali menyelesaikan satu task, tandai dengan `[x]`.
> Ikutin spesifikasi teknis di bagian paling bawah secara ketat (Kotlin 100%, Jetpack Compose, MVVM, Firebase). Jangan menambah scope di luar dokumen ini tanpa konfirmasi.
> jangan ngubah isi gradle/libs.version.toml nanti malah bentrok

---

## 📋 Ringkasan Proyek

**Nama Aplikasi:** Eirsoft
**Deskripsi:** Aplikasi manajemen stok dan persewaan properti airsoft dengan dua role pengguna:
- **Admin**: mengelola stok properti, menyetujui/menolak pengajuan peminjaman.
- **Member**: melihat katalog properti (stok tersedia), mengajukan peminjaman, memantau riwayat peminjaman sendiri.

**Arsitektur:** MVVM (Model–Repository–ViewModel–UI terpisah jelas)
**Backend:** Firebase (Authentication, Firestore, Storage untuk properti letakkan di local saja tidak usah di cloud)
**UI:** Jetpack Compose (tidak ada XML layout)
**Bahasa:** Kotlin 100%

---

## 🗂️ FASE 0 s/d FASE 12 — SELESAI
(Setup, Auth, CRUD, Profile, GPS, Detail Properti, dsb.)
- [x] Semua task di Fase 0 hingga Fase 12 telah diselesaikan.

---

## 🗂️ FASE 13 — Stock Logic Fix & Quick Actions — SELESAI
- [x] Perbaikan logika update stok: `availableStock` sekarang menyesuaikan secara otomatis saat `totalStock` diubah (menggunakan perhitungan selisih/diff).
- [x] Tambahkan fitur tombol cepat (+/-) pada input stok di `AddEditPropertyScreen`.
- [x] Pastikan `availableStock` tidak bernilai negatif dengan `coerceAtLeast(0)`.

---

## 🗂️ FASE 14 — Finishing & Polishing
- [x] Final Testing untuk semua alur (Member pinjam -> Admin approve -> Member return -> Admin receive).
- [ ] Pembersihan resource gambar lokal yang tidak terpakai saat data dihapus.
