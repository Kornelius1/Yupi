# Yupi - Panduan Pengguna & Dokumentasi

[![Download APK](https://img.shields.io/badge/Download-APK_v1.0.0-2ea44f?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Kornelius1/Yupi/releases/tag/v1.0.0)

Yupi adalah aplikasi Android yang menghitung perkiraan jumlah kata yang Anda ucapkan setiap hari, lengkap dengan metrik pola bicara. Semua pemrosesan terjadi langsung di dalam perangkat Anda. Audio tidak pernah direkam ke file dan tidak dikirim ke mana pun.

## Daftar Isi

* [Fitur Utama](#fitur-utama)
* [Cara Kerja](#cara-kerja)
* [Persyaratan Perangkat](#persyaratan-perangkat)
* [Izin Aplikasi](#izin-aplikasi)
* [Panduan Pengguna](#panduan-pengguna)
  * [1. Registrasi Suara](#1-registrasi-suara)
  * [2. Memulai dan Menghentikan Tracking](#2-memulai-dan-menghentikan-tracking)
  * [3. Membaca Ringkasan Data](#3-membaca-ringkasan-data)
  * [4. Mengatasi Aplikasi Terhenti Otomatis](#4-mengatasi-aplikasi-terhenti-otomatis)
* [Privasi dan Data](#privasi-dan-data)
* [Batasan Aplikasi](#batasan-aplikasi)
* [Pemecahan Masalah](#pemecahan-masalah)
* [Penafian](#penafian)

## Fitur Utama

* **Hitung Kata Harian**: Khusus mendeteksi suara pemilik. Suara orang lain, TV, dan musik diabaikan.
* **Registrasi Suara**: Pembuatan profil suara dilakukan langsung di perangkat Anda.
* **Metrik Pola Bicara**: Mengukur durasi bicara Anda, durasi lawan bicara, jumlah pergantian pembicara, durasi dialog vs. monolog, serta skor interaktivitas.
* **Grafik Ringkasan**: Menampilkan grafik donat perbandingan durasi bicara Anda dan lawan bicara.
* **Berjalan di Latar Belakang**: Aplikasi tetap aktif bekerja sebagai layanan latar belakang (*foreground service*) dengan notifikasi yang selalu terlihat saat tracking berjalan.
* **Penyimpanan Lokal**: Seluruh data tersimpan di HP tanpa memerlukan akses atau izin internet.

## Cara Kerja

1. Mikrofon mendeteksi suara menggunakan sistem Deteksi Aktivitas Suara (*Voice Activity Detection* / VAD).
2. Kondisi hening dan kebisingan latar belakang akan dibuang.
3. Suara dikumpulkan sampai terjadi hening sekitar 0,6 detik.
4. Sistem memverifikasi apakah suara tersebut cocok dengan profil suara Anda.
5. Jika bukan suara Anda, ucapan dicatat sebagai durasi bicara lawan bicara.
6. Jika terverifikasi sebagai suara Anda, sistem menghitung suku kata untuk memperkirakan jumlah kata dan menyimpannya ke database lokal.

## Persyaratan Perangkat

* **Sistem Operasi**: Android versi yang didukung oleh konfigurasi perangkat Anda.
* **Mikrofon**: Wajib berfungsi dengan baik.
* **Memori**: Disarankan RAM 4 GB atau lebih agar pemrosesan berjalan lancar.
* **Penyimpanan**: Membutuhkan ruang secukupnya untuk aplikasi dan model data internal.

## Izin Aplikasi

| Izin | Kegunaan | Status |
| ----- | ----- | ----- |
| **Mikrofon** | Mendengarkan suara untuk registrasi dan tracking | Wajib |
| **Layanan Latar Belakang** | Menjalankan tracking di latar belakang saat aplikasi ditutup | Wajib |
| **Mikrofon Latar Belakang** | Mengakses mikrofon di latar belakang pada Android 14+ | Wajib |
| **Notifikasi** | Menampilkan notifikasi indikator aktif pada Android 13+ | Disarankan |

Aplikasi ini tidak meminta izin internet, kontak, lokasi, kamera, maupun penyimpanan eksternal. Jika Anda menolak izin mikrofon, fitur tracking tidak dapat berjalan. Anda dapat mengaktifkannya kembali melalui **Pengaturan > Aplikasi > Yupi > Izin > Mikrofon**.

## Panduan Pengguna

### 1. Registrasi Suara

* Buka aplikasi lalu berikan izin Mikrofon dan Notifikasi saat diminta.
* Ketuk menu **Daftar Suara** di pojok kanan atas.
* Ketuk **Mulai Rekam** dan ikuti instruksi di layar. Bicaralah secara terus-menerus selama rekaman tanpa jeda di awal.
* Tunggu proses pemrosesan selesai dalam beberapa detik hingga status berubah menjadi berhasil disimpan.

**Tips Registrasi**:

* Bicaralah dengan volume dan jarak seperti biasa Anda menggunakan HP.
* Lakukan registrasi di ruangan yang tenang.
* Jika hasil tracking sering melewatkan suara Anda, lakukan registrasi ulang.

### 2. Memulai dan Menghentikan Tracking

* **Memulai**: Di halaman utama, ketuk tombol besar di tengah. Notifikasi **Yupi Aktif** akan muncul. Layar dapat dimatikan atau Anda bisa beralih ke aplikasi lain; tracking akan tetap berjalan.
* **Menghentikan**: Ketuk tombol besar sekali lagi. Notifikasi akan hilang dan tracking berhenti.

### 3. Membaca Ringkasan Data

* **Profil Suara**: Menampilkan status registrasi suara pemilik.
* **Hari Ini**: Total perkiraan kata Anda hari ini (diperbarui otomatis setiap ganti tanggal).
* **Durasi Bicara**: Perbandingan lama waktu Anda bicara dibanding lawan bicara beserta grafik donat.
* **Giliran**: Frekuensi pergantian pembicara dalam percakapan.
* **Interaktivitas**: Rata-rata pergantian pembicara per menit.
* **Dialog vs Monolog**: Perbandingan durasi percakapan dua arah dengan durasi bicara sendiri.

### 4. Mengatasi Aplikasi Terhenti Otomatis

Sistem penghemat baterai pada beberapa merek HP (seperti Tecno, Xiaomi, Oppo, Vivo, atau Samsung) sering mematikan aplikasi latar belakang. Agar Yupi tetap berjalan:

* Buka **Pengaturan > Aplikasi > Yupi > Baterai**.
* Pilih opsi **Tanpa Batasan** (*Unrestricted*).
* Hindari menutup aplikasi secara paksa dari pengelola tugas (*task manager*).

## Privasi dan Data

* **Tidak Ada Perekaman**: Audio tidak pernah disimpan ke dalam file. Audio hanya diproses di memori sementara maksimal 10 detik per ucapan lalu langsung dibuang.
* **Data Tersimpan**: Hanya jumlah kata harian dan sidik jari suara Anda dalam bentuk angka numerik yang tersimpan di penyimpanan internal HP.
* **Tanpa Koneksi Luar**: Tidak ada data yang dikirim ke server mana pun karena aplikasi tidak menggunakan izin internet.
* **Menghapus Data**: Anda dapat menghapus seluruh riwayat dan profil suara melalui **Pengaturan > Aplikasi > Yupi > Penyimpanan > Hapus Data**.
* **Etika Penggunaan**: Penggunaan mikrofon secara terus-menerus dapat menangkap percakapan di sekitar. Beri tahu orang lain jika Anda mengaktifkan aplikasi ini di ruang bersama dan patuhi aturan privasi yang berlaku.

## Batasan Aplikasi

* Angka jumlah kata merupakan hasil estimasi dari hitungan suku kata. Kecepatan bicara, bahasa, dan kondisi kejelasan suara dapat memengaruhi hasil.
* Ucapan pendek dengan durasi di bawah 1,5 detik akan diabaikan oleh sistem.
* Suara pelan, berjarak jauh, atau kondisi lingkungan yang bising dapat menurunkan akurasi verifikasi.
* Suara yang sangat mirip (seperti anggota keluarga dekat atau rekaman suara Anda sendiri) berpotensi lolos verifikasi. Aplikasi ini tidak dirancang untuk sistem keamanan atau autentikasi identitas.
* Pemrosesan mikrofon dan algoritma di latar belakang secara terus-menerus akan memengaruhi daya baterai perangkat.

## Pemecahan Masalah

| Masalah | Kemungkinan Penyebab dan Solusi |
| ----- | ----- |
| **Tombol tracking redup** | Profil suara belum didaftarkan. Buka menu **Daftar Suara**. |
| **Pesan izin mikrofon muncul** | Aktifkan izin mikrofon melalui pengaturan aplikasi di HP Anda. |
| **Notifikasi tidak muncul** | Berikan izin notifikasi pada pengaturan sistem Android. |
| **Perkiraan kata tidak bertambah** | Pastikan suara sudah terdaftar. Coba rekam ulang profil suara di lokasi yang lebih tenang. |
| **Kata orang lain ikut terhitung** | Lakukan registrasi ulang dengan rekaman suara yang lebih bersih dari gangguan. |
| **Tracking berhenti sendiri** | Fitur optimasi baterai aktif. Ubah pengaturan baterai aplikasi menjadi **Tanpa Batasan**. |
| **Aplikasi lambat saat mulai tracking** | Kondisi ini normal karena sistem membutuhkan waktu sekitar 2 detik untuk memuat model. |

## Penafian

Aplikasi ini disediakan "apa adanya" tanpa jaminan akurasi mutlak. Perkiraan jumlah kata dan metrik pola bicara bersifat indikatif, serta tidak dirancang untuk kebutuhan medis, psikologis, atau forensik.
