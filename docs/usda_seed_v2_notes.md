# Fitto USDA Seed v2 Notları

Bu paket `seed_foods.json` dosyasını v2 formatına yükseltir.

## Ne değişti?

- `seedVersion`: 2 yapıldı.
- Tekil/temel gıdalar için `source = "usda_fdc"` ve `isVerified = true` kullanıldı.
- Türk yemekleri için `source = "fitto_recipe_usda_calculated"` ve `isVerified = false` kullanıldı.
- Türk yemekleri USDA'da birebir bulunmadığı için Fitto tarif varsayımlarıyla, USDA kaynaklı bileşen değerlerinden hesaplanmış kabul edilir.
- `SeedDataImporter` hem kökü array olan JSON'u hem de `{ "foods": [...] }` formatını okuyacak hale getirildi.
- `FOOD_SEED_VERSION = 2` olduğu için daha önce v1 import edilmiş olsa bile Room'daki foods tablosu upsert ile güncellenir.

## Kurulum

1. Dosyaları aynı path'lere kopyala.
2. Android Studio'da `Build > Clean Project` ve `Build > Rebuild Project` yap.
3. Uygulamayı çalıştır.
4. Gerekirse uygulama verisini temizle; ama v1'den v2'ye geçişte normalde seed version yükseltmesi tekrar import eder.

## Kaynak politikası

USDA FoodData Central verileri public domain/CC0 olarak kullanılabilir. Uygulamada kaynak gösterimi yapılması önerilir.
TürKomp verileri şu aşamada doğrudan kopyalanmadı; ileride resmi kullanım izni/sözleşme olursa aynı seed şemasına `source = "turkomp"` olarak eklenebilir.
