# fitto seed_foods.json - v1

Bu dosya Room'a ilk kurulumda aktarılacak temel besin sözlüğünün başlangıç sürümüdür.

## Mantık

- Değerler 100 g bazlı tutulur.
- Kullanıcı porsiyon değiştirdiğinde kalori ve makrolar 100 g değerlerinden hesaplanır.
- `defaultServingAmount` sadece arayüzde hızlı başlangıç porsiyonudur.
- `dietTags` alanı, diyet planlarında hangi besinin hangi plana uygun olduğunu filtrelemek için kullanılır.
- Bu dosya ilk sürüm için yaklaşık/temsilî değerler içerir. Yayın öncesinde TürKomp, USDA FoodData Central, ürün etiketi veya Open Food Facts gibi kaynaklarla doğrulanmalıdır.

## Önerilen Room Entity alanları

```kotlin
@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey val id: String,
    val nameTr: String,
    val nameEn: String,
    val category: String,
    val defaultUnit: String,
    val defaultServingAmount: Double,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double?,
    val source: String,
    val isVerified: Boolean,
    val allergensJson: String,
    val dietTagsJson: String,
    val mealTypesJson: String
)
```

## Ölçekleme

Plan öğünlerinde `foodId` referansı kullanılacak. Kullanıcının günlük kalori hedefi 1800 kcal baz plandan farklıysa miktarlar ölçeklenecek. Sebze/yeşillik gibi düşük kalorili destek besinleri ileride `scalable=false` yapılabilir.
