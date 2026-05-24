# Fitto Diet Plan Seed v1

Bu paket hazır diyet planlarını Room'a aktarılabilir veri setine taşır.

## Dosyalar

- `seed_diet_plans.json`: plan katalog bilgileri
- `seed_planned_meals.json`: planların gün/öğün kayıtları
- `seed_planned_meal_foods.json`: her planlı öğündeki foodId + porsiyon kayıtları

## Kapsam

- Akdeniz Diyeti: 7 gün
- Ketojenik Diyet: 21 gün
- DASH Diyeti: 14 gün
- MIND Diyeti: 14 gün
- Eliminasyon Diyeti: 21 gün
- Bitki Bazlı Beslenme: 14 gün
- Sağlıklı Kilo Alma Planı: 21 gün

Tüm hazır planlar 4 öğünlüdür: kahvaltı, ara öğün, öğle yemeği ve akşam yemeği.
Kullanıcı özel diyet listesi oluşturduğunda öğün sayısı ileride dinamik hale getirilebilir.

## Veri mantığı

Planlı öğünler doğrudan yemek adı metnine değil `seed_foods.json` içindeki `foodId` değerlerine bağlanır. Böylece:

- besin değerleri merkezi foods tablosundan gelir,
- porsiyonlar kullanıcının günlük hedef kalorisine göre ölçeklenebilir,
- planlı öğün uygulandığında gerçek MealEntryFood kayıtlarına dönüştürülebilir.

`baseCalories` alanları v2 USDA ağırlıklı `seed_foods.json` değerlerinden yaklaşık hesaplanmıştır. Türk yemekleri Fitto tarif varsayımlarıyla hesaplandığı için `isVerified=false` olan food kaynakları içerebilir.

## Sonraki adım

Bir sonraki kod adımında DailyFragment içindeki plan kartı hardcoded `DietMealPlanCatalog` yerine Room'daki `planned_meals` ve `planned_meal_foods` tablolarından beslenecek.
