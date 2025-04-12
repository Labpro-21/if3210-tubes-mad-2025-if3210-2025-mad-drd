# Purrytify

## Deskripsi Aplikasi
Purrytify adalah aplikasi pemutar musik yang memungkinkan pengguna untuk mendengarkan, mengelola, dan menyukai lagu-lagu favorit mereka. Aplikasi ini menawarkan antarmuka yang intuitif dengan fitur pemutaran musik yang lengkap, termasuk mini player yang selalu tersedia. Pengguna dapat menambahkan lagu dari penyimpanan eksternal dan menikmati pengalaman musik yang menyenangkan meskipun dalam keadaan offline.

## Library yang Digunakan
- **Jetpack Compose**
- **Dagger Hilt**
- **Retrofit & OkHttp**
- **Room Database**
- **Media3 (ExoPlayer)**
- **DataStore**
- **Coil**
- **WorkManager**
- **RecyclerView**

## Screenshot Aplikasi
### Halaman Home
![Halaman Home Purrytify](/screenshot/home.png)

### Halaman Pemutar Lagu
![Halaman Pemutar Lagu](/screenshot/songplay.png)

### Halaman Library
![Halaman Library](/screenshot/library.png)

## Analisis OWASP
### M4: Insufficient Input/Output Validation
Saat analisis ditemukan kekurangan pada validasi mimetype untuk file audio song beserta artworknya dan telah diperbaiki

```kotlin
val audioFilePicker = rememberLauncherForActivityResult(
contract = ActivityResultContracts.GetContent()
) { uri: Uri? ->
uri?.let {
    audioUri = it
    audioError = null

    // Ditambahkan Setelah Analisis
    val mimeType = context.contentResolver.getType(uri)
    if (mimeType == null || !mimeType.startsWith("audio/")) {
        audioError = "Invalid audio file selected"
        return@let
    }
```
                


### M8: Security Misconfiguration
Saat analisis tidak ditemukan security misconfiguration

### M9: Insecure Data Storage
Saat analisis tidak ditemukan Insecure Data Storage 

## Pembagian Kerja Anggota Kelompok
| Nama | NIM | Tugas |
|------|-----|-------|
| Dewantoro Triatmojo | 13522011 | Header dan Navbar, Library, Putar Lagu, Tambah Lagu, Profile, Liked Songs, Network |
| Randy Verdian | 13522067 | Home, Repeat, Shuffle, Queue |
| Abdul Rafi Radityo Hutomo | 13522089 | Background Process, Library, Testing, OWASP,  |

## Jumlah Jam Persiapan dan Pengerjaan
| Nama | Jam Persiapan | Jam Pengerjaan | Total |
|------|---------------|----------------|-------|
| Dewantoro Triatmojo | 2 | 15 | 17 |
| Randy Verdian | 2 | 12 | 14 |
| Abdul Rafi Radityo Hutomo | 2 | 12 | 14 |