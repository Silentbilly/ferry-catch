export type Lang = 'en' | 'tr' | 'ru'

export function normalizeLang(value: unknown): Lang {
  if (value === 'tr' || value === 'ru' || value === 'en') return value
  return 'en'
}

export function getLocale(lang: Lang): string {
  switch (lang) {
    case 'tr':
      return 'tr-TR'
    case 'ru':
      return 'ru-RU'
    case 'en':
    default:
      return 'en-GB'
  }
}

export const messages = {
  en: {
    pageTitle: 'Istanbul Ferries (Islands)',
    loadingStops: 'Loading stops…',
    failedToLoadStops: 'Failed to load stops',
    route: 'Route',
    from: 'From',
    to: 'To',
    selectPlaceholder: 'Select…',
    searching: 'Searching…',
    findNext: 'Find next',
    nextFerry: 'Next Ferry',
    more: 'More →',
    in: 'In:',
    dep: 'Dep:',
    arr: 'Arr:',
    operator: 'Operator:',
    windMap: 'Wind map',
    moreAbout: 'More about ferries & schedule',
    departed: 'departed',
    min: 'min',
    hour: 'h',

    back: '← Back',
    loading: 'Loading…',
    failedToLoadData: 'Failed to load data',
    timetable: 'Timetable',
    direct: 'Direct',
    stop: 'stop',
    stops: 'stops',
    inSmall: 'in',

    seo: {
      paragraph1:
        "If you are looking for an Istanbul to Princes' Islands ferry (Adalar), here you can quickly see the next departures from ports like Kabataş, Kadıköy, Beşiktaş, Bostancı, Maltepe and Kartal.",
      paragraph2:
        "Ferries to Adalar (Princes' Islands) run throughout the day, and this Istanbul ferries schedule helps you find the next departure without checking multiple websites.",
      paragraph3:
        'Each operator uses its own pier, so always check the information boards and ask on site to make sure the ferry you need departs from that pier and arrives at the correct island.',
      paragraph4:
        'Here you can quickly check the Istanbul ferries schedule for your route: departure and arrival times, intermediate stops and operators, then open full details for a specific sailing when you need more information.',
      paragraph5:
        'In case of strong wind or adverse weather conditions, ferry operators may cancel or change departures and routes, so always double-check the latest notices on official operator websites before you travel.',
      operatorsPrefix: 'Official operators:',
      operatorsSeparator: ', ',
      operatorsLastSeparator: ' or ',
    },
  },

  tr: {
    pageTitle: 'İstanbul Vapurları (Adalar)',
    loadingStops: 'İskeleler yükleniyor…',
    failedToLoadStops: 'İskeleler yüklenemedi',
    route: 'Rota',
    from: 'Nereden',
    to: 'Nereye',
    selectPlaceholder: 'Seçin…',
    searching: 'Aranıyor…',
    findNext: 'Sonrakini bul',
    nextFerry: 'Sıradaki vapur',
    more: 'Detay →',
    in: 'Kalan:',
    dep: 'Kalkış:',
    arr: 'Varış:',
    operator: 'İşletmeci:',
    windMap: 'Rüzgâr haritası',
    moreAbout: 'Vapurlar ve saatler hakkında daha fazla bilgi',
    departed: 'kalktı',
    min: 'dk',
    hour: 'sa',

    back: '← Geri',
    loading: 'Yükleniyor…',
    failedToLoadData: 'Veriler yüklenemedi',
    timetable: 'Sefer saatleri',
    direct: 'Direkt',
    stop: 'durak',
    stops: 'durak',
    inSmall: 'kalan',

    seo: {
      paragraph1:
        "İstanbul'dan Adalar vapurunu arıyorsanız, burada Kabataş, Kadıköy, Beşiktaş, Bostancı, Maltepe ve Kartal gibi iskelelerden bir sonraki seferleri hızlıca görebilirsiniz.",
      paragraph2:
        "Adalar'a vapurlar gün boyunca çalışır ve bu İstanbul vapur saatleri sayfası, birden fazla siteyi kontrol etmeden sonraki seferi bulmanıza yardımcı olur.",
      paragraph3:
        "Her işletmeci farklı bir iskeleyi kullanabildiği için, ihtiyacınız olan vapurun doğru iskeleden kalktığını ve doğru adaya gittiğini teyit etmek adına bilgi panolarını kontrol edin ve gerekirse görevliye sorun.",
      paragraph4:
        'Burada hattınız için kalkış ve varış saatlerini, ara durakları ve işletmecileri hızlıca kontrol edebilir; daha fazla bilgi gerektiğinde belirli bir seferin detaylarını açabilirsiniz.',
      paragraph5:
        'Kuvvetli rüzgâr veya olumsuz hava koşullarında işletmeciler seferleri iptal edebilir ya da güzergâhları değiştirebilir; bu yüzden yolculuktan önce resmî duyuruları mutlaka tekrar kontrol edin.',
      operatorsPrefix: 'Resmî işletmeciler:',
      operatorsSeparator: ', ',
      operatorsLastSeparator: ' ve ',
    },
  },

  ru: {
    pageTitle: 'Паромы Стамбула (Острова)',
    loadingStops: 'Загрузка остановок…',
    failedToLoadStops: 'Не удалось загрузить остановки',
    route: 'Маршрут',
    from: 'Откуда',
    to: 'Куда',
    selectPlaceholder: 'Выберите…',
    searching: 'Поиск…',
    findNext: 'Найти ближайший',
    nextFerry: 'Ближайший паром',
    more: 'Подробнее →',
    in: 'Через:',
    dep: 'Отпр.:',
    arr: 'Приб.:',
    operator: 'Оператор:',
    windMap: 'Карта ветра',
    moreAbout: 'Подробнее о паромах и расписании',
    departed: 'рейс ушёл',
    min: 'мин',
    hour: 'ч',

    back: '← Назад',
    loading: 'Загрузка…',
    failedToLoadData: 'Не удалось загрузить данные',
    timetable: 'Расписание',
    direct: 'Прямой',
    stop: 'остановка',
    stops: 'остановок',
    inSmall: 'через',

    seo: {
      paragraph1:
        'Если вы ищете паром из Стамбула на Принцевы острова (Adalar), здесь можно быстро посмотреть ближайшие отправления из Кабаташа, Кадыкёя, Бешикташа, Бостанджы, Малтепе и Картала.',
      paragraph2:
        'Паромы на Adalar ходят в течение всего дня, и это расписание паромов Стамбула помогает найти ближайший рейс без проверки нескольких сайтов.',
      paragraph3:
        'У каждого перевозчика может быть свой причал, поэтому всегда проверяйте табло и уточняйте на месте, что нужный вам паром отправляется именно от этого причала и идёт на нужный остров.',
      paragraph4:
        'Здесь можно быстро посмотреть расписание паромов по вашему маршруту: время отправления и прибытия, промежуточные остановки и перевозчиков, а затем открыть полную информацию по конкретному рейсу.',
      paragraph5:
        'При сильном ветре или плохой погоде перевозчики могут отменять рейсы или менять маршруты, поэтому перед поездкой обязательно перепроверьте последние объявления на официальных сайтах.',
      operatorsPrefix: 'Официальные перевозчики:',
      operatorsSeparator: ', ',
      operatorsLastSeparator: ' или ',
    },
  },
} as const