# Fitto Merge Notes

Bu birleşimde `fittochoncc.zip` ana kaynak kabul edildi. `fitto - final (2).zip` içinden sadece yeni/istenen alanlar ana projeye entegre edildi.

## Taşınan başlıca yenilikler

- Tarifler sekmesi ve ilgili tüm `feature_recipes` ekranları
- Tarif verisi (`assets/recipes.json`), `RecipeEntity`, `RecipeDao`, `RecipeRepository` ve Room converter entegrasyonu
- Profilim sekmesindeki geliştirilmiş profil ekranları, kilo/vücut ölçüleri, albüm, başarılar ve hatırlatıcı altyapısı
- Sosyal taraftaki ek ekranlar ve bildirim/draft/post detay entegrasyonları
- Bildirim kanalları, yerel bildirim deposu, hatırlatıcı receiver/service/worker sınıfları
- Gerekli drawable/layout/menu/string/color/dimens/style kaynakları

## Korunan ana proje tarafları

- Günlük sekmesinin ana proje sürümü korundu; sadece bildirimden ilgili karta yönlendirme metodu eklendi.
- Diyet planları, öğün, egzersiz, uyku, Health Connect, USDA/LogMeal, favori yemek ve profil cache gibi 1. projede olup 2. projede eksik/gerilemiş parçalar korundu.
- `app/build.gradle.kts` ana proje ayarları korundu; yalnızca Glide ve WorkManager bağımlılıkları eklendi.
- Room veritabanı ana projedeki entity/DAO’ları koruyacak şekilde tarif entity/DAO ile genişletildi ve versiyon `6` yapıldı.

## Kontrol notu

Bu ortamda Gradle dağıtımı internetten indirilemediği için tam Android build çalıştırılamadı (`services.gradle.org` erişilemedi). Buna rağmen XML parse kontrolü, kaynak referans kontrolü ve proje içi sınıf/import kontrolü yapıldı; görülen eksikler giderildi.
