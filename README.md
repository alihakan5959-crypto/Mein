# NetPulse Android 0.2

Oyun ve diğer uygulamalar üzerinde taşınabilen ağ + görüntü değişimi monitörü.

## Kullanım
1. APK'yı kurup NetPulse'u aç; Pencere izni ile üstte gösterme iznini ver.
2. Yalnız ağ sayacı için Ölçümü başlat düğmesini kullan.
3. Görüntü analizi için Ekran + veri analizi başlat düğmesine bas ve Android'in ekran paylaşımını onayla.
4. Oyununa dön; kayan penceredeki Bölge seç düğmesine bas.
5. İzlenecek oyun alanını parmağınla çizip Kullan'a bas.
6. Pencereyi başlığından sürükle. − ile küçült. İçerik uzunsa pencere içinde kaydır.
7. Sayaçları sıfırla hem trafik oturumunu hem görüntü karşılaştırmasını yeniden başlatır.
8. Taramayı durdur yalnız ekran analizini durdurur. × veya bildirimde Durdur tüm servisi kapatır.

## Ölçülenler
- Tüm telefonda gelen/giden bayt ve IP paketleri; B/KB/MB ve bayt/sn.
- Seçilen alanda, ardışık görüntülerde değişen örnek piksel oranı (%).
- Her ağ ölçüm aralığına düşen geçerli görüntü karşılaştırmalarının ortalaması ve örnek sayısı.
- Yeşil ağ trafiği ve mavi görüntü değişimi, aynı gerçek zaman ekseninde.
- Yeşil eksen otomatik veri hızına göre, mavi eksen sabit %0–100 aralığında ölçeklenir.
- En çok 60 ölçüm aralığı bellekte tutulur.

## Görüntü analizi nasıl çalışır?
Android MediaProjection ile kullanıcı onaylı tam ekran görüntüsü alınır.
Görüntünün en fazla 360 piksel genişliğindeki kopyası, seçilen alanda 48x32 RGB noktada örneklenir.
İki örnekte RGB kanallarından en az biri 24/255 veya daha fazla değişirse o nokta değişmiş sayılır.
Hedef en fazla 4 örnek/sn'dir; bu oyun FPS'si değildir. Gerçek karşılaştırma sayısı pencerede görünür.
Kayan pencerenin kapladığı alan ve kenar gölgesi karşılaştırmadan çıkarılır.
Ekran örnekleri yalnız cihaz belleğinde işlenir, dosyaya kaydedilmez ve dışarı gönderilmez.
Alan seçimi sırasında analiz duraklar; seçimden sonra önceki karşılaştırma temizlenir.
1,5 saniyeden uzun görüntü boşlukları iki uzak kareyi hareketmiş gibi karşılaştırmaz.
Görüntü örneği yoksa, bölge tamamen pencere altındaysa veya karşılaştırma için yeterli nokta yoksa oran — gösterilir; sıfır uydurulmaz.
Ekran döndürülürse tarama durur; yeni oturum için yeniden ekran izni verilir.
Android, ekran kilidinde veya kullanıcı paylaşımı durdurduğunda taramayı sonlandırabilir.

## Sınırlar
- Ağ sayaçları TÜM TELEFONUN trafiğini kapsar. Sadece seçilen oyun/pencereyi kapsamaz.
- Ağ ve görüntünün aynı anda değişmesi, gelen verinin animasyon dosyası olduğunu kanıtlamaz.
- Paket sayısı dosya, animasyon parçası veya kare sayısı değildir.
- Animasyonun indirilen dosya boyutu, sayısı, gelecekteki süresi ve oyun sonucu ölçülmez/tahmin edilmez.
- Hareket yüzdesi, örneklenmiş noktalardaki renk değişimidir; nesne takibi veya gerçek FPS değildir.
- Korumalı içerik siyah görünebilir. Uygulama ekran korumasını aşmaz.
- VPN/sanal arayüzler üst üste sayıma yol açabilir; fatura ölçümü değildir.
- Arka plan/sistem trafiği toplamda görünür. Android sayaçları toplu güncelleyebilir.
- Ekran kapalıyken zamanlama azaltılabilir; hız gerçek geçen süreye bölünür.
- İnternet, mikrofon, erişilebilirlik veya VPN izni istemez. Ekran analizi için her oturumda Android ekran paylaşımı izni ister.

## Derleme ve doğrulama
JDK 17, Gradle 8.9, Android SDK 35:
`gradle --no-daemon testDebugUnitTest lintDebug assembleDebug`

Testler ağ sayaçlarının zamanlama/sıfırlama koşullarını; RGBA satır dolgusunu, bölge kırpmasını, pencere maskelemesini, renk farkı eşiğini, eksik örnekleri ve zaman aralığı eşleşmesini kapsar.
GitHub Actions imzalı debug APK'yı NetPulse-Android ZIP çıktısına koyar.
CI sanal makinesindeki testler, gerçek telefon ekran paylaşımı/izin testinin yerine geçmez.
Her CI çalışması yeni debug anahtarı üretebilir; önceki APK ile imza uyuşmazlığında eski sürümü kaldırıp yeniden kur.
