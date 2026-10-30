package de.mef.lebensmittel

/** App languages. Product names stay German (they come from the database). */
enum class Lang(val code: String, val nativeName: String, val rtl: Boolean = false, val decimalComma: Boolean = true) {
    UK("uk", "Українська"),
    EN("en", "English", decimalComma = false),
    DE("de", "Deutsch"),
    RU("ru", "Русский"),
    AR("ar", "العربية", rtl = true, decimalComma = false);

    companion object {
        fun fromCode(code: String?): Lang? = entries.firstOrNull { it.code == code }
    }
}

object I18n {
    /** Current language; set by MainActivity from the settings. */
    @Volatile
    var lang: Lang = Lang.UK

    /** "system" -> phone language if supported, otherwise English. */
    fun resolve(setting: String?): Lang = when (setting) {
        null, "", "system" -> Lang.fromCode(java.util.Locale.getDefault().language) ?: Lang.EN
        else -> Lang.fromCode(setting) ?: Lang.UK
    }
}

/** One text in all languages. `T.x()` gives it in the current language, `{0}`, `{1}` are filled from the arguments. */
class Tr(val uk: String, val en: String, val de: String, val ru: String, val ar: String) {
    fun get(lang: Lang = I18n.lang): String = when (lang) {
        Lang.UK -> uk
        Lang.EN -> en
        Lang.DE -> de
        Lang.RU -> ru
        Lang.AR -> ar
    }

    operator fun invoke(vararg args: Any?): String {
        var s = get()
        args.forEachIndexed { i, a -> s = s.replace("{$i}", a?.toString() ?: "") }
        return s
    }

    override fun toString(): String = get()
}

private fun t(uk: String, en: String, de: String, ru: String, ar: String) = Tr(uk, en, de, ru, ar)

/** All texts of the app. Order of every line: Ukrainian, English, German, Russian, Arabic. */
object T {
    // ---------- general ----------
    val appTitle = t("Список продуктів", "Shopping list", "Einkaufsliste", "Список покупок", "قائمة التسوق")
    val tabLists = t("Списки", "Lists", "Listen", "Списки", "القوائم")
    val tabNutrition = t("Склад", "Nutrition", "Nährwerte", "Состав", "المكونات")
    val tabSpending = t("Витрати", "Spending", "Ausgaben", "Расходы", "المصاريف")
    val tabSettings = t("Налаштування", "Settings", "Einstellungen", "Настройки", "الإعدادات")
    val menu = t("Меню", "Menu", "Menü", "Меню", "القائمة")
    val back = t("Назад", "Back", "Zurück", "Назад", "رجوع")
    val cancel = t("Скасувати", "Cancel", "Abbrechen", "Отмена", "إلغاء")
    val close = t("Закрити", "Close", "Schließen", "Закрыть", "إغلاق")
    val done = t("Готово", "Done", "Fertig", "Готово", "تم")
    val add = t("Додати", "Add", "Hinzufügen", "Добавить", "إضافة")
    val save = t("Зберегти", "Save", "Speichern", "Сохранить", "حفظ")
    val delete = t("Видалити", "Delete", "Löschen", "Удалить", "حذف")
    val clear = t("Очистити", "Clear", "Leeren", "Очистить", "مسح")
    val share = t("Поділитися", "Share", "Teilen", "Поделиться", "مشاركة")
    val today = t("Сьогодні", "Today", "Heute", "Сегодня", "اليوم")
    val find = t("Знайти", "Search", "Suchen", "Найти", "بحث")
    val error = t("помилка", "error", "Fehler", "ошибка", "خطأ")

    // ---------- dates ----------
    val relToday = t("сьогодні", "today", "heute", "сегодня", "اليوم")
    val relTomorrow = t("завтра", "tomorrow", "morgen", "завтра", "غدًا")
    val relYesterday = t("вчора", "yesterday", "gestern", "вчера", "أمس")

    // ---------- units ----------
    val unitPcs = t("шт", "pcs", "Stk.", "шт", "قطعة")
    val unitG = t("г", "g", "g", "г", "غ")
    val unitKg = t("кг", "kg", "kg", "кг", "كغ")
    val unitMl = t("мл", "ml", "ml", "мл", "مل")
    val unitL = t("л", "l", "l", "л", "لتر")
    val unitPack = t("уп.", "pack", "Pck.", "уп.", "عبوة")
    val unitMg = t("мг", "mg", "mg", "мг", "ملغ")
    val unitUg = t("мкг", "µg", "µg", "мкг", "ميكروغرام")
    val kcal = t("ккал", "kcal", "kcal", "ккал", "سعرة")
    val kj = t("кДж", "kJ", "kJ", "кДж", "كيلوجول")

    // ---------- databases ----------
    val srcBeauty = t("гігієна", "hygiene", "Drogerie", "гигиена", "نظافة")
    val srcProducts = t("побутове", "household", "Haushalt", "бытовое", "منزلية")
    val srcPet = t("для тварин", "pets", "Tierbedarf", "для животных", "للحيوانات")

    // ---------- calendar ----------
    val pasteFromClipboard = t("Вставити список з буфера", "Paste list from clipboard", "Liste aus Zwischenablage einfügen", "Вставить список из буфера", "لصق القائمة من الحافظة")
    val pasteReceived = t("Вставити отриманий список", "Paste a received list", "Empfangene Liste einfügen", "Вставить полученный список", "لصق قائمة مستلمة")
    val spendingPeriod = t("Витрати за період", "Spending for a period", "Ausgaben im Zeitraum", "Расходы за период", "المصاريف خلال فترة")
    val prevMonth = t("Попередній місяць", "Previous month", "Vorheriger Monat", "Предыдущий месяц", "الشهر السابق")
    val nextMonth = t("Наступний місяць", "Next month", "Nächster Monat", "Следующий месяц", "الشهر التالي")
    val noListsYet = t(
        "Ще немає жодного списку. Виберіть день у календарі або натисніть «Сьогодні».",
        "No lists yet. Pick a day in the calendar or tap “Today”.",
        "Noch keine Listen. Wähle einen Tag im Kalender oder tippe auf „Heute“.",
        "Пока нет ни одного списка. Выберите день в календаре или нажмите «Сегодня».",
        "لا توجد قوائم بعد. اختر يومًا من التقويم أو اضغط «اليوم».",
    )
    val listSummary = t("{0}, куплено {1}: {2}", "{0}, {1} bought: {2}", "{0}, {1} gekauft: {2}", "{0}, куплено {1}: {2}", "{0}، تم شراء {1}: {2}")

    // ---------- day ----------
    val boughtOf = t("куплено {0} з {1}", "{0} of {1} bought", "{0} von {1} gekauft", "куплено {0} из {1}", "تم شراء {0} من {1}")
    val copyFromOtherDay = t("Скопіювати з іншого дня", "Copy from another day", "Von anderem Tag kopieren", "Скопировать из другого дня", "نسخ من يوم آخر")
    val copyListText = t("Копіювати список (текст і посилання)", "Copy list (text and link)", "Liste kopieren (Text und Link)", "Копировать список (текст и ссылка)", "نسخ القائمة (نص ورابط)")
    val removeBought = t("Прибрати куплене", "Remove bought items", "Gekauftes entfernen", "Убрать купленное", "إزالة ما تم شراؤه")
    val clearList = t("Очистити список", "Clear list", "Liste leeren", "Очистить список", "مسح القائمة")
    val clearListQ = t("Очистити список?", "Clear the list?", "Liste leeren?", "Очистить список?", "مسح القائمة؟")
    val clearListText = t("Усі продукти на {0} буде видалено.", "All products for {0} will be removed.", "Alle Produkte für {0} werden gelöscht.", "Все продукты на {0} будут удалены.", "سيتم حذف جميع منتجات {0}.")
    val dayEmpty = t("Список на цей день порожній", "The list for this day is empty", "Die Liste für diesen Tag ist leer", "Список на этот день пуст", "قائمة هذا اليوم فارغة")
    val addProduct = t("Додати продукт", "Add product", "Produkt hinzufügen", "Добавить продукт", "إضافة منتج")
    val editQtyPrice = t("Змінити кількість або ціну", "Change quantity or price", "Menge oder Preis ändern", "Изменить количество или цену", "تعديل الكمية أو السعر")
    val sumLine = t("Сума: {0}", "Total: {0}", "Summe: {0}", "Сумма: {0}", "المجموع: {0}")
    val boughtFor = t(" · куплено на {0}", " · bought: {0}", " · gekauft: {0}", " · куплено на {0}", " · تم الشراء: {0}")
    val totalApprox = t("Разом ≈ {0}", "Total ≈ {0}", "Gesamt ≈ {0}", "Итого ≈ {0}", "الإجمالي ≈ {0}")
    val unknownKcal = t(
        " (без {0} поз. з невідомою калорійністю)",
        " (excl. {0} items with unknown calories)",
        " (ohne {0} Pos. mit unbekannten Kalorien)",
        " (без {0} поз. с неизвестной калорийностью)",
        " (باستثناء {0} عناصر بسعرات غير معروفة)",
    )

    // ---------- add product ----------
    val inListCount = t("{0} · у списку: {1}", "{0} · on the list: {1}", "{0} · auf der Liste: {1}", "{0} · в списке: {1}", "{0} · في القائمة: {1}")
    val scanBarcode = t("Сканувати штрихкод", "Scan barcode", "Barcode scannen", "Сканировать штрихкод", "مسح الباركود")
    val searchPlaceholder = t("хліб, молоко, Brot…", "bread, milk, Brot…", "Brot, Milch, Käse…", "хлеб, молоко, Brot…", "bread، milk، Brot…")
    val frequent = t("Часто купую", "Frequently bought", "Oft gekauft", "Часто покупаю", "أشتريها كثيرًا")
    val startTypingHint = t(
        "Почніть вводити назву українською, німецькою, російською чи англійською (наприклад «хліб», «Brot» або «bread»), або відскануйте штрихкод кнопкою вгорі.",
        "Start typing a product name in English or German (e.g. “bread” or “Brot”) or scan a barcode with the button at the top.",
        "Gib einen Produktnamen ein (z. B. „Brot“) oder scanne einen Barcode mit der Taste oben.",
        "Начните вводить название по-русски, по-немецки, по-украински или по-английски (например «хлеб», «Brot» или «bread») или отсканируйте штрихкод кнопкой вверху.",
        "ابدأ بكتابة اسم المنتج بالإنجليزية أو الألمانية (مثل «bread» أو «Brot») أو امسح الباركود بالزر في الأعلى.",
    )
    val addPlain = t("Додати просто «{0}»", "Just add “{0}”", "Einfach „{0}“ hinzufügen", "Просто добавить «{0}»", "إضافة «{0}» فقط")
    val withoutChoosing = t("без вибору конкретного товару", "without choosing a specific product", "ohne ein bestimmtes Produkt zu wählen", "без выбора конкретного товара", "دون اختيار منتج محدد")
    val myProducts = t("Мої продукти", "My products", "Meine Produkte", "Мои продукты", "منتجاتي")
    val dictionary = t("Словник", "Dictionary", "Wörterbuch", "Словарь", "القاموس")
    val dbProducts = t("Товари з бази (продукти, гігієна, побутове)", "From the database (food, hygiene, household)", "Aus der Datenbank (Lebensmittel, Drogerie, Haushalt)", "Товары из базы (продукты, гигиена, бытовое)", "من قاعدة البيانات (طعام، نظافة، منزلية)")
    val searchFor = t("Шукати «{0}»", "Search “{0}”", "„{0}“ suchen", "Искать «{0}»", "ابحث عن «{0}»")
    val inStore = t(" в {0}", " at {0}", " bei {0}", " в {0}", " في {0}")
    val resultsFor = t("Показано результати для «{0}»", "Showing results for “{0}”", "Ergebnisse für „{0}“", "Показаны результаты для «{0}»", "نتائج «{0}»")
    val tapToChoose = t("натисніть, щоб вибрати конкретний товар", "tap to pick a specific product", "tippen, um ein Produkt zu wählen", "нажмите, чтобы выбрать конкретный товар", "اضغط لاختيار منتج محدد")
    val justThis = t("Просто так", "Just this", "Einfach so", "Просто так", "فقط هذا")
    val storeLabel = t("Магазин:", "Store:", "Markt:", "Магазин:", "المتجر:")
    val allStores = t("Усі магазини", "All stores", "Alle Märkte", "Все магазины", "كل المتاجر")
    val noStoreInfo = t("У знайдених товарах магазин не вказано", "No store info in these results", "Bei diesen Treffern ist kein Markt angegeben", "В найденных товарах магазин не указан", "لا توجد معلومات عن المتجر في هذه النتائج")

    // ---------- messages ----------
    val tooManySearches = t(
        "Забагато пошуків за хвилину. Зачекайте трохи й спробуйте ще раз.",
        "Too many searches in a minute. Please wait a moment and try again.",
        "Zu viele Suchen pro Minute. Bitte kurz warten und erneut versuchen.",
        "Слишком много поисков за минуту. Подождите немного и попробуйте снова.",
        "عمليات بحث كثيرة خلال دقيقة. انتظر قليلًا ثم حاول مرة أخرى.",
    )
    val noConnection = t("Немає зв'язку з базою. Перевірте інтернет.", "No connection to the database. Check your internet.", "Keine Verbindung zur Datenbank. Bitte Internet prüfen.", "Нет связи с базой. Проверьте интернет.", "لا يوجد اتصال بقاعدة البيانات. تحقق من الإنترنت.")
    val noneInStore = t(
        "Серед товарів «{0}» немає позначених магазином {1}. Магазин указано не в усіх товарів, тож можна вибрати «Усі магазини».",
        "None of the “{0}” products is marked with {1}. Not all products have a store, so you can choose “All stores”.",
        "Keines der „{0}“-Produkte ist mit {1} markiert. Nicht bei allen Produkten ist ein Markt angegeben – wähle ggf. „Alle Märkte“.",
        "Среди товаров «{0}» нет отмеченных магазином {1}. Магазин указан не у всех товаров, можно выбрать «Все магазины».",
        "لا يوجد بين منتجات «{0}» ما هو مُعلَّم بالمتجر {1}. المتجر غير مذكور لكل المنتجات، يمكنك اختيار «كل المتاجر».",
    )
    val nothingFound = t(
        "Нічого не знайдено для «{0}»{1}. Спробуйте інше слово або додайте просто так.",
        "Nothing found for “{0}”{1}. Try another word or just add it.",
        "Nichts gefunden für „{0}“{1}. Versuche ein anderes Wort oder füge es einfach so hinzu.",
        "Ничего не найдено для «{0}»{1}. Попробуйте другое слово или добавьте просто так.",
        "لم يتم العثور على «{0}»{1}. جرّب كلمة أخرى أو أضفه كما هو.",
    )
    val inStoreShort = t(" у {0}", " at {0}", " bei {0}", " в {0}", " في {0}")
    val added = t("Додано: {0} — {1}", "Added: {0} — {1}", "Hinzugefügt: {0} — {1}", "Добавлено: {0} — {1}", "تمت الإضافة: {0} — {1}")
    val enterBarcode = t("Введіть штрихкод (8 або 13 цифр)", "Enter a barcode (8 or 13 digits)", "Barcode eingeben (8 oder 13 Ziffern)", "Введите штрихкод (8 или 13 цифр)", "أدخل الباركود (8 أو 13 رقمًا)")
    val barcodeNotFoundAnywhere = t(
        "Штрихкод {0} не знайдено в жодній базі (продукти, гігієна, побутове, корм).",
        "Barcode {0} was not found in any database (food, hygiene, household, pet food).",
        "Barcode {0} wurde in keiner Datenbank gefunden (Lebensmittel, Drogerie, Haushalt, Tierfutter).",
        "Штрихкод {0} не найден ни в одной базе (продукты, гигиена, бытовое, корм).",
        "لم يتم العثور على الباركود {0} في أي قاعدة بيانات (طعام، نظافة، منزلية، طعام الحيوانات).",
    )
    val cantOpenPicture = t("Не вдалося відкрити картинку", "Could not open the picture", "Bild konnte nicht geöffnet werden", "Не удалось открыть картинку", "تعذر فتح الصورة")
    val cantReadBarcode = t("Не вдалося прочитати штрихкод", "Could not read the barcode", "Barcode konnte nicht gelesen werden", "Не удалось прочитать штрихкод", "تعذرت قراءة الباركود")
    val scannerUnavailable = t("Сканер недоступний: {0}", "Scanner not available: {0}", "Scanner nicht verfügbar: {0}", "Сканер недоступен: {0}", "الماسح غير متاح: {0}")
    val barcodeNotFoundEnterName = t(
        "Штрихкод {0} не знайдено в базі. Введіть назву, і штрихкод запам'ятається.",
        "Barcode {0} is not in the database. Type a name and the barcode will be remembered.",
        "Barcode {0} ist nicht in der Datenbank. Gib einen Namen ein, der Barcode wird gespeichert.",
        "Штрихкод {0} не найден в базе. Введите название, и штрихкод запомнится.",
        "الباركود {0} غير موجود في القاعدة. اكتب الاسم وسيتم حفظ الباركود.",
    )
    val listEmpty = t("Список порожній", "The list is empty", "Die Liste ist leer", "Список пуст", "القائمة فارغة")
    val shareSubject = t("Список продуктів на {0}", "Shopping list for {0}", "Einkaufsliste für {0}", "Список покупок на {0}", "قائمة التسوق ليوم {0}")
    val shareChooser = t("Поділитися списком", "Share list", "Liste teilen", "Поделиться списком", "مشاركة القائمة")
    val listCopied = t("Список скопійовано", "List copied", "Liste kopiert", "Список скопирован", "تم نسخ القائمة")
    val copiedFrom = t("Скопійовано {0} з {1}", "Copied {0} from {1}", "{0} von {1} kopiert", "Скопировано {0} из {1}", "تم نسخ {0} من {1}")
    val noListLink = t("Не знайдено посилання на список продуктів", "No shopping list link found", "Kein Link zu einer Einkaufsliste gefunden", "Ссылка на список покупок не найдена", "لم يتم العثور على رابط قائمة تسوق")
    val clipboardEmpty = t("Буфер обміну порожній. Скопіюйте повідомлення зі списком.", "The clipboard is empty. Copy the message with the list.", "Die Zwischenablage ist leer. Kopiere die Nachricht mit der Liste.", "Буфер обмена пуст. Скопируйте сообщение со списком.", "الحافظة فارغة. انسخ الرسالة التي تحتوي على القائمة.")
    val addedCount = t("Додано {0}", "Added {0}", "{0} hinzugefügt", "Добавлено {0}", "تمت إضافة {0}")

    // ---------- share text ----------
    val shareHeader = t("🛒 Список продуктів на {0}", "🛒 Shopping list for {0}", "🛒 Einkaufsliste für {0}", "🛒 Список покупок на {0}", "🛒 قائمة التسوق ليوم {0}")
    val shareTotal = t("Разом: {0}", "Total: {0}", "Summe: {0}", "Итого: {0}", "المجموع: {0}")
    val openInApp = t("Відкрити в додатку Lebensmittel:", "Open in the Lebensmittel app:", "In der App Lebensmittel öffnen:", "Открыть в приложении Lebensmittel:", "افتح في تطبيق Lebensmittel:")

    // ---------- dialogs ----------
    val less = t("Менше", "Less", "Weniger", "Меньше", "أقل")
    val more = t("Більше", "More", "Mehr", "Больше", "أكثر")
    val quantity = t("Кількість", "Quantity", "Menge", "Количество", "الكمية")
    val packWeightUnknown = t(
        "Вага упаковки невідома, тому загальну калорійність не пораховано. Вкажіть кількість у г або кг.",
        "The package weight is unknown, so total calories can't be calculated. Enter the amount in g or kg.",
        "Das Packungsgewicht ist unbekannt, daher keine Gesamtkalorien. Gib die Menge in g oder kg an.",
        "Вес упаковки неизвестен, поэтому общая калорийность не посчитана. Укажите количество в г или кг.",
        "وزن العبوة غير معروف، لذا لم تُحسب السعرات الإجمالية. أدخل الكمية بالغرام أو الكيلوغرام.",
    )
    val priceLabel = t("Ціна разом, € (необов'язково)", "Total price, € (optional)", "Gesamtpreis, € (optional)", "Цена всего, € (необязательно)", "السعر الإجمالي، € (اختياري)")
    val enterNumber = t("Введіть число, наприклад 2,49", "Enter a number, e.g. 2.49", "Gib eine Zahl ein, z. B. 2,49", "Введите число, например 2,49", "أدخل رقمًا، مثل 2.49")
    val perUnit = t("≈ {0} за 1 {1}", "≈ {0} per 1 {1}", "≈ {0} pro 1 {1}", "≈ {0} за 1 {1}", "≈ {0} لكل 1 {1}")
    val filledFromLast = t("Підставлено за останньою ціною", "Filled in from the last price", "Vom letzten Preis übernommen", "Подставлено по последней цене", "تمت التعبئة من آخر سعر")
    val canAddLater = t("Можна вказати пізніше, кнопкою ✎ у списку", "You can add it later with the ✎ button in the list", "Kann später über ✎ in der Liste eingetragen werden", "Можно указать позже кнопкой ✎ в списке", "يمكنك إضافته لاحقًا بزر ✎ في القائمة")
    val receivedList = t("Отримано список", "List received", "Liste empfangen", "Получен список", "تم استلام قائمة")
    val composedFor = t(" · складений на {0}", " · made for {0}", " · erstellt für {0}", " · составлен на {0}", " · مُعدّة ليوم {0}")
    val addToDay = t("Додати на день:", "Add to day:", "Zu Tag hinzufügen:", "Добавить на день:", "إضافة إلى يوم:")
    val copyFromDay = t("Скопіювати з дня", "Copy from day", "Von Tag kopieren", "Скопировать из дня", "نسخ من يوم")
    val noOtherLists = t("Інших списків поки немає.", "There are no other lists yet.", "Es gibt noch keine anderen Listen.", "Других списков пока нет.", "لا توجد قوائم أخرى بعد.")
    val appBackground = t("Фон додатка", "App background", "App-Hintergrund", "Фон приложения", "خلفية التطبيق")
    val pickFromGallery = t("Вибрати з галереї", "Choose from gallery", "Aus Galerie wählen", "Выбрать из галереи", "اختيار من المعرض")
    val pickOtherPicture = t("Вибрати іншу картинку", "Choose another picture", "Anderes Bild wählen", "Выбрать другую картинку", "اختيار صورة أخرى")
    val removeBackground = t("Прибрати фон", "Remove background", "Hintergrund entfernen", "Убрать фон", "إزالة الخلفية")
    val dimHint = t("Затемнення картинки, щоб текст було добре видно", "Dim the picture so the text stays readable", "Bild abdunkeln, damit der Text gut lesbar ist", "Затемнение картинки, чтобы текст было хорошо видно", "تعتيم الصورة لتبقى النصوص واضحة")
    val brighter = t("яскравіше", "brighter", "heller", "ярче", "أكثر سطوعًا")
    val calmer = t("спокійніше", "calmer", "ruhiger", "спокойнее", "أهدأ")

    // ---------- settings ----------
    val appearance = t("Вигляд", "Appearance", "Darstellung", "Внешний вид", "المظهر")
    val language = t("Мова", "Language", "Sprache", "Язык", "اللغة")
    val languageSystem = t("Як у системі", "System", "Wie im System", "Как в системе", "لغة النظام")
    val theme = t("Тема", "Theme", "Design", "Тема", "السمة")
    val themeLight = t("Світла", "Light", "Hell", "Светлая", "فاتحة")
    val themeDark = t("Темна", "Dark", "Dunkel", "Тёмная", "داكنة")
    val themeSystem = t("Як у системі", "System", "Wie im System", "Как в системе", "حسب النظام")
    val bgAutoTextHint = t(
        "Поки стоїть картинка на фоні, колір тексту підбирається сам: на темній картинці написи світлі, на світлій темні.",
        "While a background picture is set, the text colour is chosen automatically: light text on dark pictures, dark text on light ones.",
        "Solange ein Hintergrundbild gesetzt ist, wird die Textfarbe automatisch gewählt: heller Text auf dunklen Bildern, dunkler auf hellen.",
        "Пока на фоне стоит картинка, цвет текста подбирается сам: на тёмной картинке надписи светлые, на светлой тёмные.",
        "عند وجود صورة خلفية يُختار لون النص تلقائيًا: نص فاتح على الصور الداكنة ونص داكن على الصور الفاتحة.",
    )
    val bgPick = t("Картинка на фон з галереї…", "Background picture from gallery…", "Hintergrundbild aus der Galerie…", "Картинка на фон из галереи…", "صورة خلفية من المعرض…")
    val bgChange = t("Змінити або прибрати картинку фону…", "Change or remove background picture…", "Hintergrundbild ändern oder entfernen…", "Изменить или убрать картинку фона…", "تغيير صورة الخلفية أو إزالتها…")
    val nameColor = t("Колір назв продуктів", "Product name colour", "Farbe der Produktnamen", "Цвет названий продуктов", "لون أسماء المنتجات")
    val colorCode = t("Код кольору (#RRGGBB)", "Colour code (#RRGGBB)", "Farbcode (#RRGGBB)", "Код цвета (#RRGGBB)", "رمز اللون (#RRGGBB)")
    val defaultColor = t("Стандартний колір", "Default colour", "Standardfarbe", "Стандартный цвет", "اللون الافتراضي")
    val whatToShow = t("Що показувати у списку", "What to show in the list", "Was in der Liste angezeigt wird", "Что показывать в списке", "ما يُعرض في القائمة")
    val showPhotos = t("Фото продуктів", "Product photos", "Produktfotos", "Фото продуктов", "صور المنتجات")
    val showBrand = t("Фірма і вага упаковки", "Brand and package size", "Marke und Packungsgröße", "Фирма и вес упаковки", "العلامة التجارية وحجم العبوة")
    val showKcal = t("Калорійність", "Calories", "Kalorien", "Калорийность", "السعرات الحرارية")
    val settingsListHint = t(
        "Ці налаштування стосуються списку на день. У пошуку деталі видно завжди, щоб було легше вибрати товар.",
        "These settings apply to the day list. Search results always show details to make choosing easier.",
        "Diese Einstellungen gelten für die Tagesliste. In der Suche werden Details immer angezeigt.",
        "Эти настройки касаются списка на день. В поиске детали видны всегда, чтобы легче было выбрать товар.",
        "تنطبق هذه الإعدادات على قائمة اليوم. تظهر التفاصيل دائمًا في البحث لتسهيل الاختيار.",
    )

    // ---------- spending ----------
    val fromDate = t("від {0}", "from {0}", "ab {0}", "с {0}", "من {0}")
    val toDate = t("до {0}", "to {0}", "bis {0}", "по {0}", "إلى {0}")
    val onlyBought = t("Лише куплене (з галочкою)", "Only bought (ticked)", "Nur Gekauftes (abgehakt)", "Только купленное (с галочкой)", "المشتريات فقط (المؤشَّر عليها)")
    val spendStats = t(
        "{0} дн. · в середньому {1} на день · покупок: {2}",
        "{0} days · average {1} per day · purchases: {2}",
        "{0} Tage · Ø {1} pro Tag · Einkäufe: {2}",
        "{0} дн. · в среднем {1} в день · покупок: {2}",
        "{0} يوم · المتوسط {1} يوميًا · المشتريات: {2}",
    )
    val withoutPrice = t("Без ціни: {0} поз. (не враховано в сумі)", "Without price: {0} items (not counted)", "Ohne Preis: {0} Pos. (nicht mitgezählt)", "Без цены: {0} поз. (не учтено в сумме)", "بدون سعر: {0} عناصر (غير محتسبة)")
    val noPricedPurchases = t(
        "За цей період немає покупок із ціною. Ціну можна вказати, коли додаєте продукт, або пізніше кнопкою ✎ у списку.",
        "No purchases with a price in this period. You can enter a price when adding a product or later with ✎ in the list.",
        "Keine Einkäufe mit Preis in diesem Zeitraum. Preise kannst du beim Hinzufügen oder später über ✎ eintragen.",
        "За этот период нет покупок с ценой. Цену можно указать при добавлении продукта или позже кнопкой ✎ в списке.",
        "لا توجد مشتريات بسعر في هذه الفترة. يمكنك إدخال السعر عند إضافة المنتج أو لاحقًا بزر ✎ في القائمة.",
    )
    val byDay = t("По днях", "By day", "Nach Tagen", "По дням", "حسب الأيام")
    val topSpending = t("Найбільше витрат", "Biggest spending", "Größte Ausgaben", "Больше всего расходов", "أكبر المصاريف")
    val presetWeek = t("Цей тиждень", "This week", "Diese Woche", "Эта неделя", "هذا الأسبوع")
    val presetMonth = t("Цей місяць", "This month", "Dieser Monat", "Этот месяц", "هذا الشهر")
    val presetLastMonth = t("Минулий місяць", "Last month", "Letzter Monat", "Прошлый месяц", "الشهر الماضي")
    val preset30 = t("30 днів", "30 days", "30 Tage", "30 дней", "30 يومًا")
    val presetYear = t("Цей рік", "This year", "Dieses Jahr", "Этот год", "هذا العام")

    // ---------- nutrition ----------
    val nutritionTitle = t("Склад продукту", "Product info", "Produktinfo", "Состав продукта", "معلومات المنتج")
    val enterBarcodeManually = t("або введіть штрихкод вручну", "or type the barcode", "oder Barcode eingeben", "или введите штрихкод вручную", "أو أدخل الباركود يدويًا")
    val nutritionIntro = t(
        "Відскануйте товар, щоб побачити калорійність, білки, жири, вуглеводи, вітаміни, склад, алергени, добавки і Nutri-Score — усе, що є про нього в базі.",
        "Scan a product to see calories, protein, fat, carbs, vitamins, ingredients, allergens, additives and Nutri-Score — everything the database knows.",
        "Scanne ein Produkt, um Kalorien, Eiweiß, Fett, Kohlenhydrate, Vitamine, Zutaten, Allergene, Zusatzstoffe und Nutri-Score zu sehen.",
        "Отсканируйте товар, чтобы увидеть калорийность, белки, жиры, углеводы, витамины, состав, аллергены, добавки и Nutri-Score — всё, что о нём есть в базе.",
        "امسح المنتج لترى السعرات والبروتين والدهون والكربوهيدرات والفيتامينات والمكونات ومسببات الحساسية والإضافات وNutri-Score.",
    )
    val recentlyChecked = t("Нещодавно перевірені", "Recently checked", "Zuletzt geprüft", "Недавно проверенные", "تم فحصها مؤخرًا")
    val nova1 = t("необроблений або мінімально оброблений продукт", "unprocessed or minimally processed food", "unverarbeitetes oder minimal verarbeitetes Lebensmittel", "необработанный или минимально обработанный продукт", "طعام غير مُصنَّع أو قليل المعالجة")
    val nova2 = t("кулінарний інгредієнт", "processed culinary ingredient", "verarbeitete Küchenzutat", "кулинарный ингредиент", "مكوّن طهي مُعالج")
    val nova3 = t("оброблений продукт", "processed food", "verarbeitetes Lebensmittel", "обработанный продукт", "طعام مُصنَّع")
    val nova4 = t("ультраоброблений продукт", "ultra-processed food", "hochverarbeitetes Lebensmittel", "ультраобработанный продукт", "طعام فائق التصنيع")
    val noNutrition = t("Поживної цінності для цього товару в базі немає.", "No nutrition facts for this product in the database.", "Für dieses Produkt sind keine Nährwerte in der Datenbank.", "Пищевой ценности для этого товара в базе нет.", "لا توجد قيم غذائية لهذا المنتج في القاعدة.")
    val nutritionFacts = t("Поживна цінність", "Nutrition facts", "Nährwerte", "Пищевая ценность", "القيم الغذائية")
    val serving = t("порція", "serving", "Portion", "порция", "حصة")
    val ingredients = t("Склад", "Ingredients", "Zutaten", "Состав", "المكونات")
    val allergens = t("Алергени", "Allergens", "Allergene", "Аллергены", "مسببات الحساسية")
    val mayContain = t("Може містити сліди", "May contain traces of", "Kann Spuren enthalten von", "Может содержать следы", "قد يحتوي على آثار من")
    val additives = t("Харчові добавки", "Additives", "Zusatzstoffe", "Пищевые добавки", "المضافات الغذائية")
    val addToToday = t("Додати в список на сьогодні", "Add to today's list", "Zur heutigen Liste hinzufügen", "Добавить в список на сегодня", "إضافة إلى قائمة اليوم")
    val dataNote = t(
        "Дані: Open Food Facts та суміжні бази. Їх заповнюють користувачі, тому вони можуть бути неповними.",
        "Data: Open Food Facts and related databases. Filled in by users, so it may be incomplete.",
        "Daten: Open Food Facts und verwandte Datenbanken. Von Nutzern gepflegt, daher evtl. unvollständig.",
        "Данные: Open Food Facts и связанные базы. Их заполняют пользователи, поэтому они могут быть неполными.",
        "البيانات: Open Food Facts وقواعد مرتبطة. يملؤها المستخدمون لذا قد تكون ناقصة.",
    )
    val noName = t("Без назви", "No name", "Ohne Namen", "Без названия", "بدون اسم")

    // ---------- reminders ----------
    val tabReminders = t("Нагадування", "Reminders", "Erinnerungen", "Напоминания", "التذكيرات")
    val reminderNew = t("Нове нагадування", "New reminder", "Neue Erinnerung", "Новое напоминание", "تذكير جديد")
    val reminderEdit = t("Змінити нагадування", "Edit reminder", "Erinnerung bearbeiten", "Изменить напоминание", "تعديل التذكير")
    val reminderText = t("Текст нагадування", "Reminder text", "Text der Erinnerung", "Текст напоминания", "نص التذكير")
    val reminderDay = t("День", "Day", "Tag", "День", "اليوم")
    val reminderTime = t("Час", "Time", "Uhrzeit", "Время", "الوقت")
    val noReminders = t(
        "На цей день нагадувань немає. Натисніть «+», щоб додати.",
        "No reminders for this day. Tap “+” to add one.",
        "Für diesen Tag gibt es keine Erinnerungen. Tippe auf „+“, um eine hinzuzufügen.",
        "На этот день напоминаний нет. Нажмите «+», чтобы добавить.",
        "لا توجد تذكيرات لهذا اليوم. اضغط «+» لإضافة تذكير.",
    )
    val upcoming = t("Найближчі нагадування", "Upcoming reminders", "Anstehende Erinnerungen", "Ближайшие напоминания", "التذكيرات القادمة")
    val reminderPast = t("минуло", "past", "vorbei", "прошло", "انتهى")
    val reminderOn = t("Увімкнено", "On", "An", "Включено", "مفعّل")
    val notificationsOff = t(
        "Сповіщення вимкнені, тому нагадування не прийдуть.",
        "Notifications are off, so reminders won't arrive.",
        "Benachrichtigungen sind aus – Erinnerungen kommen nicht an.",
        "Уведомления выключены, поэтому напоминания не придут.",
        "الإشعارات متوقفة، لذلك لن تصل التذكيرات.",
    )
    val allow = t("Дозволити", "Allow", "Erlauben", "Разрешить", "سماح")
    val exactAlarmsOff = t(
        "Точні будильники не дозволені: нагадування може прийти на кілька хвилин пізніше.",
        "Exact alarms are not allowed: reminders may arrive a few minutes late.",
        "Exakte Wecker sind nicht erlaubt: Erinnerungen können ein paar Minuten später kommen.",
        "Точные будильники не разрешены: напоминание может прийти на несколько минут позже.",
        "المنبهات الدقيقة غير مسموح بها: قد يصل التذكير متأخرًا بضع دقائق.",
    )
    val reminderSaved = t("Нагадування: {0}, {1}", "Reminder: {0}, {1}", "Erinnerung: {0}, {1}", "Напоминание: {0}, {1}", "تذكير: {0}، {1}")
    val reminderInPast = t(
        "Цей час уже минув, сповіщення не прийде.",
        "This time has already passed, so there will be no notification.",
        "Diese Zeit ist schon vorbei, es kommt keine Benachrichtigung.",
        "Это время уже прошло, уведомление не придёт.",
        "هذا الوقت قد مضى، لذلك لن يصل إشعار.",
    )
    val enterText = t("Введіть текст", "Enter some text", "Text eingeben", "Введите текст", "أدخل نصًا")
    val tabsSection = t("Вкладки", "Tabs", "Tabs", "Вкладки", "علامات التبويب")
    val tabsHint = t(
        "«Списки» видно завжди, налаштування відкриваються через ⋮ на головному екрані. Вимкнена вкладка просто ховається: дані лишаються, нагадування й далі приходять.",
        "“Lists” is always shown; settings open from ⋮ on the main screen. A hidden tab keeps its data, and reminders still arrive.",
        "„Listen“ ist immer sichtbar, die Einstellungen öffnest du über ⋮ auf dem Hauptbildschirm. Ausgeblendete Tabs behalten ihre Daten, Erinnerungen kommen weiterhin.",
        "«Списки» видны всегда, настройки открываются через ⋮ на главном экране. Скрытая вкладка сохраняет данные, напоминания продолжают приходить.",
        "«القوائم» تظهر دائمًا، وتُفتح الإعدادات من ⋮ في الشاشة الرئيسية. علامة التبويب المخفية تحتفظ ببياناتها وتستمر التذكيرات بالوصول.",
    )

    // ---------- plurals ----------
    /** "5 продуктів", "5 products", "5 Produkte", "5 продуктов", "5 منتجات" */
    fun productsCount(n: Int): String = when (I18n.lang) {
        Lang.UK -> "$n " + slavic(n, "продукт", "продукти", "продуктів")
        Lang.RU -> "$n " + slavic(n, "продукт", "продукта", "продуктов")
        Lang.EN -> if (n == 1) "1 product" else "$n products"
        Lang.DE -> if (n == 1) "1 Produkt" else "$n Produkte"
        Lang.AR -> when {
            n == 1 -> "منتج واحد"
            n == 2 -> "منتجان"
            n % 100 in 3..10 -> "$n منتجات"
            else -> "$n منتجًا"
        }
    }

    private fun slavic(n: Int, one: String, few: String, many: String): String {
        val m10 = n % 10
        val m100 = n % 100
        return when {
            m10 == 1 && m100 != 11 -> one
            m10 in 2..4 && m100 !in 12..14 -> few
            else -> many
        }
    }

    // ---------- calendar names ----------
    fun weekdaysShort(): List<String> = when (I18n.lang) {
        Lang.UK -> listOf("пн", "вт", "ср", "чт", "пт", "сб", "нд")
        Lang.RU -> listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
        Lang.EN -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        Lang.DE -> listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
        Lang.AR -> listOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")
    }

    /** Very short names for the calendar header (7 narrow columns). */
    fun weekdaysHeader(): List<String> = when (I18n.lang) {
        Lang.AR -> listOf("إث", "ث", "أر", "خ", "ج", "س", "أح")
        else -> weekdaysShort()
    }

    fun monthsNominative(): List<String> = when (I18n.lang) {
        Lang.UK -> listOf("Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень", "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень")
        Lang.RU -> listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
        Lang.EN -> listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        Lang.DE -> listOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember")
        Lang.AR -> listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
    }

    /** Month name inside a date ("30 жовтня", "30 октября"). */
    fun monthsInDate(): List<String> = when (I18n.lang) {
        Lang.UK -> listOf("січня", "лютого", "березня", "квітня", "травня", "червня", "липня", "серпня", "вересня", "жовтня", "листопада", "грудня")
        Lang.RU -> listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
        else -> monthsNominative()
    }

    // ---------- nutrients ----------
    private val nutrientNames: Map<String, Tr> = mapOf(
        "energy-kcal" to t("Енергія", "Energy", "Energie", "Энергия", "الطاقة"),
        "energy-kj" to t("Енергія", "Energy", "Energie", "Энергия", "الطاقة"),
        "fat" to t("Жири", "Fat", "Fett", "Жиры", "الدهون"),
        "saturated-fat" to t("з них насичені", "of which saturates", "davon gesättigte Fettsäuren", "из них насыщенные", "منها مشبعة"),
        "monounsaturated-fat" to t("мононенасичені", "monounsaturates", "einfach ungesättigte", "мононенасыщенные", "أحادية غير مشبعة"),
        "polyunsaturated-fat" to t("поліненасичені", "polyunsaturates", "mehrfach ungesättigte", "полиненасыщенные", "متعددة غير مشبعة"),
        "trans-fat" to t("трансжири", "trans fat", "Transfette", "трансжиры", "دهون متحولة"),
        "cholesterol" to t("Холестерин", "Cholesterol", "Cholesterin", "Холестерин", "الكوليسترول"),
        "carbohydrates" to t("Вуглеводи", "Carbohydrates", "Kohlenhydrate", "Углеводы", "الكربوهيدرات"),
        "sugars" to t("з них цукри", "of which sugars", "davon Zucker", "из них сахара", "منها سكريات"),
        "added-sugars" to t("доданий цукор", "added sugars", "zugesetzter Zucker", "добавленный сахар", "سكريات مضافة"),
        "lactose" to t("лактоза", "lactose", "Laktose", "лактоза", "اللاكتوز"),
        "polyols" to t("поліоли", "polyols", "mehrwertige Alkohole", "полиолы", "بوليولات"),
        "starch" to t("крохмаль", "starch", "Stärke", "крахмал", "النشا"),
        "fiber" to t("Харчові волокна", "Fibre", "Ballaststoffe", "Пищевые волокна", "الألياف"),
        "proteins" to t("Білки", "Protein", "Eiweiß", "Белки", "البروتين"),
        "salt" to t("Сіль", "Salt", "Salz", "Соль", "الملح"),
        "sodium" to t("Натрій", "Sodium", "Natrium", "Натрий", "الصوديوم"),
        "alcohol" to t("Алкоголь", "Alcohol", "Alkohol", "Алкоголь", "الكحول"),
        "caffeine" to t("Кофеїн", "Caffeine", "Koffein", "Кофеин", "الكافيين"),
        "taurine" to t("Таурин", "Taurine", "Taurin", "Таурин", "التورين"),
        "water" to t("Вода", "Water", "Wasser", "Вода", "الماء"),
        "omega-3-fat" to t("Омега-3", "Omega-3", "Omega-3", "Омега-3", "أوميغا 3"),
        "omega-6-fat" to t("Омега-6", "Omega-6", "Omega-6", "Омега-6", "أوميغا 6"),
        "omega-9-fat" to t("Омега-9", "Omega-9", "Omega-9", "Омега-9", "أوميغا 9"),
        "vitamin-a" to t("Вітамін A", "Vitamin A", "Vitamin A", "Витамин A", "فيتامين A"),
        "beta-carotene" to t("Бета-каротин", "Beta-carotene", "Beta-Carotin", "Бета-каротин", "بيتا كاروتين"),
        "vitamin-d" to t("Вітамін D", "Vitamin D", "Vitamin D", "Витамин D", "فيتامين D"),
        "vitamin-e" to t("Вітамін E", "Vitamin E", "Vitamin E", "Витамин E", "فيتامين E"),
        "vitamin-k" to t("Вітамін K", "Vitamin K", "Vitamin K", "Витамин K", "فيتامين K"),
        "vitamin-c" to t("Вітамін C", "Vitamin C", "Vitamin C", "Витамин C", "فيتامين C"),
        "vitamin-b1" to t("Вітамін B1 (тіамін)", "Vitamin B1 (thiamin)", "Vitamin B1 (Thiamin)", "Витамин B1 (тиамин)", "فيتامين B1 (ثيامين)"),
        "vitamin-b2" to t("Вітамін B2 (рибофлавін)", "Vitamin B2 (riboflavin)", "Vitamin B2 (Riboflavin)", "Витамин B2 (рибофлавин)", "فيتامين B2 (ريبوفلافين)"),
        "vitamin-pp" to t("Вітамін B3 (ніацин)", "Vitamin B3 (niacin)", "Vitamin B3 (Niacin)", "Витамин B3 (ниацин)", "فيتامين B3 (نياسين)"),
        "vitamin-b6" to t("Вітамін B6", "Vitamin B6", "Vitamin B6", "Витамин B6", "فيتامين B6"),
        "vitamin-b9" to t("Вітамін B9 (фолієва кислота)", "Vitamin B9 (folic acid)", "Vitamin B9 (Folsäure)", "Витамин B9 (фолиевая кислота)", "فيتامين B9 (حمض الفوليك)"),
        "folates" to t("Фолати", "Folates", "Folate", "Фолаты", "الفولات"),
        "vitamin-b12" to t("Вітамін B12", "Vitamin B12", "Vitamin B12", "Витамин B12", "فيتامين B12"),
        "biotin" to t("Біотин", "Biotin", "Biotin", "Биотин", "البيوتين"),
        "pantothenic-acid" to t("Пантотенова кислота", "Pantothenic acid", "Pantothensäure", "Пантотеновая кислота", "حمض البانتوثينيك"),
        "potassium" to t("Калій", "Potassium", "Kalium", "Калий", "البوتاسيوم"),
        "calcium" to t("Кальцій", "Calcium", "Calcium", "Кальций", "الكالسيوم"),
        "phosphorus" to t("Фосфор", "Phosphorus", "Phosphor", "Фосфор", "الفوسفور"),
        "iron" to t("Залізо", "Iron", "Eisen", "Железо", "الحديد"),
        "magnesium" to t("Магній", "Magnesium", "Magnesium", "Магний", "المغنيسيوم"),
        "zinc" to t("Цинк", "Zinc", "Zink", "Цинк", "الزنك"),
        "copper" to t("Мідь", "Copper", "Kupfer", "Медь", "النحاس"),
        "manganese" to t("Марганець", "Manganese", "Mangan", "Марганец", "المنغنيز"),
        "fluoride" to t("Фтор", "Fluoride", "Fluorid", "Фтор", "الفلورايد"),
        "selenium" to t("Селен", "Selenium", "Selen", "Селен", "السيلينيوم"),
        "chromium" to t("Хром", "Chromium", "Chrom", "Хром", "الكروم"),
        "molybdenum" to t("Молібден", "Molybdenum", "Molybdän", "Молибден", "الموليبدينوم"),
        "iodine" to t("Йод", "Iodine", "Jod", "Йод", "اليود"),
        "chloride" to t("Хлориди", "Chloride", "Chlorid", "Хлориды", "الكلوريد"),
        "bicarbonate" to t("Гідрокарбонати", "Bicarbonate", "Hydrogencarbonat", "Гидрокарбонаты", "البيكربونات"),
        "silica" to t("Кремній", "Silica", "Kieselsäure", "Кремний", "السيليكا"),
        "sulphate" to t("Сульфати", "Sulphate", "Sulfat", "Сульфаты", "الكبريتات"),
        "nitrate" to t("Нітрати", "Nitrate", "Nitrat", "Нитраты", "النترات"),
        "erythritol" to t("Еритритол", "Erythritol", "Erythrit", "Эритритол", "الإريثريتول"),
    )

    fun nutrientName(key: String): String = nutrientNames[key]?.get() ?: pretty(key)

    private val allergenNames: Map<String, Tr> = mapOf(
        "milk" to t("молоко", "milk", "Milch", "молоко", "الحليب"),
        "gluten" to t("глютен", "gluten", "Gluten", "глютен", "الغلوتين"),
        "eggs" to t("яйця", "eggs", "Eier", "яйца", "البيض"),
        "nuts" to t("горіхи", "nuts", "Schalenfrüchte", "орехи", "المكسرات"),
        "peanuts" to t("арахіс", "peanuts", "Erdnüsse", "арахис", "الفول السوداني"),
        "soybeans" to t("соя", "soy", "Soja", "соя", "الصويا"),
        "fish" to t("риба", "fish", "Fisch", "рыба", "السمك"),
        "crustaceans" to t("ракоподібні", "crustaceans", "Krebstiere", "ракообразные", "القشريات"),
        "molluscs" to t("молюски", "molluscs", "Weichtiere", "моллюски", "الرخويات"),
        "celery" to t("селера", "celery", "Sellerie", "сельдерей", "الكرفس"),
        "mustard" to t("гірчиця", "mustard", "Senf", "горчица", "الخردل"),
        "sesame-seeds" to t("кунжут", "sesame", "Sesam", "кунжут", "السمسم"),
        "sulphur-dioxide-and-sulphites" to t("сульфіти", "sulphites", "Sulfite", "сульфиты", "الكبريتيت"),
        "lupin" to t("люпин", "lupin", "Lupinen", "люпин", "الترمس"),
    )

    fun allergenName(tag: String): String = allergenNames[tag]?.get() ?: pretty(tag)

    private val labelNames: Map<String, Tr> = mapOf(
        "organic" to t("органічне (Bio)", "organic", "Bio", "органическое (Bio)", "عضوي"),
        "eu-organic" to t("Bio ЄС", "EU organic", "EU-Bio", "Bio ЕС", "عضوي أوروبي"),
        "vegan" to t("веганське", "vegan", "vegan", "веганское", "نباتي صرف"),
        "vegetarian" to t("вегетаріанське", "vegetarian", "vegetarisch", "вегетарианское", "نباتي"),
        "gluten-free" to t("без глютену", "gluten-free", "glutenfrei", "без глютена", "خالٍ من الغلوتين"),
        "no-lactose" to t("без лактози", "lactose-free", "laktosefrei", "без лактозы", "خالٍ من اللاكتوز"),
        "lactose-free" to t("без лактози", "lactose-free", "laktosefrei", "без лактозы", "خالٍ من اللاكتوز"),
        "fair-trade" to t("Fairtrade", "Fairtrade", "Fairtrade", "Fairtrade", "تجارة عادلة"),
        "no-added-sugar" to t("без доданого цукру", "no added sugar", "ohne Zuckerzusatz", "без добавленного сахара", "بدون سكر مضاف"),
        "palm-oil-free" to t("без пальмової олії", "palm-oil free", "ohne Palmöl", "без пальмового масла", "خالٍ من زيت النخيل"),
        "halal" to t("халяль", "halal", "halal", "халяль", "حلال"),
        "kosher" to t("кошерне", "kosher", "koscher", "кошерное", "كوشر"),
    )

    fun isKnownLabel(tag: String): Boolean = tag in labelNames
    fun labelName(tag: String): String = labelNames[tag]?.get() ?: pretty(tag)

    /** "sesame-seeds" -> "Sesame seeds" */
    fun pretty(tag: String): String = tag.substringAfter(':').replace('-', ' ').replaceFirstChar { it.uppercaseChar() }
}
