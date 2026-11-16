# NetPulse Android

Telefonun ağ trafiğini oyun ve diğer uygulamalar üzerinde taşınabilen bir pencerede izler.

## Kullanım
1. APK'yı kurup NetPulse'u aç.
2. "Pencere izni" düğmesinden diğer uygulamaların üzerinde gösterme iznini ver.
3. "Ölçümü başlat" düğmesine bas ve oyununa dön.
4. Pencereyi üst başlığından sürükle. Küçült ile tek satıra indir; Sıfırla ile yeni ölçüm başlat; Kapat ile servisi durdur.

## Ölçülenler
- Saniyelik gelen/giden bayt, otomatik B/KB/MB birimi.
- Oturum toplamları ve gelen/giden IP paketleri.
- Son 60 örnekte gelen trafik grafiği ve otomatik üst sınır.
- Her örnek gerçek geçen süreye bölünür; yaklaşık her saniye yenilenir.

## Kapsam ve sınırlar
Sayaçlar TÜM TELEFONUN ağ arayüzlerini kapsar; yalnızca oyuna ait değildir.
VPN ve sanal arayüzlerde üst üste sayım olabilir. Fatura ölçümü değildir.
IP paketi sayısı dosya, animasyon parçası veya kare sayısı değildir.
Şifreli içerikler incelenmez. Animasyon adedi, boyutu, süresi ve oyunun gelecek sonucu bu sayaçlardan belirlenmez.
Arka plan/sistem trafiği de toplamda görünür. Android veri sayaçlarını toplu güncelleyebilir.
Ekran kapandığında ölçüm sıklığı işletim sistemi tarafından azaltılabilir; hız hesabı gerçek süreyi kullanır.
Uygulama internet, erişilebilirlik, ekran kaydı veya VPN izni istemez; sayaçları cihazda okur.

## Derleme
JDK 17, Gradle 8.9 ve Android SDK 35 ile:
`gradle --no-daemon testDebugUnitTest lintDebug assembleDebug`

GitHub Actions bu kontrolleri çalıştırır ve imzalı debug APK'yı **NetPulse-Android** adlı ZIP çıktısına koyar.
Bu bir deneme sürümüdür. Gerçek telefonda izin/pencere davranışları ayrıca denenmelidir.
