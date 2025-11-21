package com.company;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FactService {
    private static final List<String> FACTS = new ArrayList<>();
    private static final Random R = new Random();

    static {
        FACTS.add("Inson miyasi tana og‘irligining taxminan 2% ni tashkil etsa ham, energiyaning 20% dan ortig‘ini ishlatadi.");
        FACTS.add("Dunyo bo‘ylab eng ko‘p gapiriladigan tildan tashqari, ko‘p tillarda bir xil so‘zlar mavjud.");
        FACTS.add("Bambuk tez o‘sadi — baʼzi turlari bir kunda bir metrgacha o‘sadi.");
        FACTS.add("Arslonlar guruhda yashamaydi — ularga 'pride' deyiladi, ammo ko‘pincha oilaviy guruhlar hosil bo‘ladi.");
        FACTS.add("Ko‘zlar ham asab tolalari orqali miyaga to‘g‘ridan-to‘g‘ri bog‘langan yagona organlardan biridir.");
        FACTS.add("To‘g‘ri o‘qiyotgan kitobni egallagan qo‘l orqali ko‘proq maʼlumot eslab qolish mumkin.");
        FACTS.add("Delfinlar o‘zini ko‘zgu orqali taniy olgan hayvonlardandir.");
        FACTS.add("Tovus shahvoniy holatda emas, balki jinsiy tanlov tufayli indamaydi — erkaklar o‘zining rang-barang quyruqlarini ko‘rsatadi.");
        FACTS.add("Qulupnayni pishib yetilishdan oldin ham gullarida hisoblangan urug‘lar mavjud.");
        FACTS.add("Ayollar o‘rtacha erkaklardan ko‘proq hid sezish qobiliyatiga ega.");
        FACTS.add("Yerning ichki qismidagi temir va nikel yadrosi magnit maydon hosil qiladi.");
        FACTS.add("Qahva 15-asrda Arab yarimorolidan toptilgan va dastlab dori sifatida ishlatilgan.");
        FACTS.add("Somon maydonida eski fermentsiya, qovurilgan don aroma hosil qiladi.");
        FACTS.add("Dunyodagi eng uzun daryo Nil va Amazon o‘rtasida bahs mavjud; uzunlikni o‘lchash qiyin.");
        FACTS.add("Karstli hududlarda yer osti daryolari va g‘orlar hosil bo‘ladi.");
        FACTS.add("Limon kislotasi tish emaliga zarar yetkazishi mumkin, shuning uchun iste'moldan keyin suv ichish tavsiya etiladi.");
        FACTS.add("Qushlar «uyqusiz» emas: ular qisqa muddatlarda hushyorlik bilan uyquni bo‘lib o‘tiradi.");
        FACTS.add("Yulduzlar ham tug‘iladi va o‘ladi — bu jarayon milliardlab yillar davom etadi.");
        FACTS.add("Qalamga o‘xshash daraxtning yog‘i qimmatbaho va parfyumeriyada ishlatiladi.");
        FACTS.add("Orqa miya nerv tolalari jarohatlanganda, baʼzi vazifalar tiklanmasligi mumkin.");
        FACTS.add("Muz qoplangan ayrim ko‘llarda aralashmalar sezilarli optik effekt beradi.");
        FACTS.add("Odamlar odatda dam olish paytida eng ko‘p ibtidoiy fikrlaringni eslaydi.");
        FACTS.add("Baʼzi baliq turlari elektr maydon hosil qilib ovqat topadi.");
        FACTS.add("Yashil choy antioksidantlarga boy, bu sog‘lik uchun foydali.");
        FACTS.add("Kundalik kichik yurishlar yurak sog‘ligi uchun katta foyda beradi.");
        FACTS.add("Baʼzi o‘simliklarning barglaridan tabiiy bo‘yoq olinadi.");
        FACTS.add("Tovug‘ning yurishi va parvoz qobiliyati genetik jihatdan aniq tartiblangan.");
        FACTS.add("Kichik bolalar tez til o‘rganadi, chunki ularning nevrologik plastisligi yuqori.");
        FACTS.add("Qor kuchli yomg‘ir va shamol bilan turli shaklga kiradi.");
        FACTS.add("Soyabon ustiga tushgan quyosh nuri ranglarni yanada jonlantiradi.");
        FACTS.add("Musaffo tungi osmonda yulduzlarni ko‘rish insonga xotirjamlik beradi.");
        FACTS.add("Baʼzi yangi texnologiyalar qayta ishlash imkoniyatlarini oshiradi.");
        FACTS.add("Planetalar o‘z yulduziga yaqinligi va massasi bilan farq qiladi.");
        FACTS.add("O‘simliklar orasida bir-biri bilan kimyoviy signallar orqali muloqot qiladiganlar bor.");
        FACTS.add("Biroq odamlar ko‘pincha ko‘zgu orqali o‘z ovozidan norozi bo‘ladi.");
        FACTS.add("Afrikadagi baʼzi daraxtlar tibbiyotda ishlatiladi.");
        FACTS.add("Asal ari qobiliyati va murakkab jamiyat tuzilmasi bilan ajralib turadi.");
        FACTS.add("Yerning qobig‘i turli geologik plastikalardan iborat.");
        FACTS.add("Kichik miqdordagi kofe tonusni oshiradi, haddan ortiq esa zarar.");
        FACTS.add("Odamlarda qaerda bo‘lishiga qarab tanlov jihatidan farq seziladi.");
        FACTS.add("Samolyot qanotlari fizik qonunlarga asoslangan holda lift hosil qiladi.");
        FACTS.add("Baʼzi metallarning yuqori haroratda xossasi o‘zgaradi.");
        FACTS.add("Inson tanasi turli mikroorganizmlar bilan koloniya hosil qiladi.");
        FACTS.add("Doimiy o‘qish miyani kengaytiradi va so‘z boyligini oshiradi.");
        FACTS.add("Quyosh nurlari teri uchun D vitamini sintezini rag'batlantiradi.");
        FACTS.add("Quyidagi fakt ham qiziq: baʼzi chigitlar hatto suv ostida ham yoshashga moslashgan.");
        FACTS.add("Delfinlar o‘z nomlariga o‘xshash tovushlarni ishlatadi deb hisoblanadi.");
        FACTS.add("Uzoq muddatli stress immunitetni zaiflashtirishi mumkin.");
        FACTS.add("Tonggi sho‘r havoda yurish kayfiyatni ko‘tarsa ham, sog‘lom ovqat muhim.");
        FACTS.add("Ayrim bakteriyalar azot aylanishida muhim rol o‘ynaydi.");
        FACTS.add("Tungi ko‘chalar shaharga alohida estetik beradi.");
        FACTS.add("Baʼzi gullar faqat muayyan industriyalarga muvofiq ochiladi.");
        FACTS.add("Ko‘pgina okean organizmlari hali o‘rganilmagan.");
        FACTS.add("Inson tanasida taxminan trillionlab hujayra mavjud.");
        FACTS.add("Erta tongda ishlash samaradorlikni oshirishi mumkin.");
        FACTS.add("Bayroq va milliy ramzlar odamlarni birlashtiruvchi rol o‘ynaydi.");
        FACTS.add("Baʼzi podsholik hayvonlar migratsiya yo‘llarini yodda saqlaydi.");
        FACTS.add("Insonning qiziqishi yangi maʼlumotlarni tez o‘rganishiga yordam beradi.");
        FACTS.add("Chayqalish va harakat koordinatsiyani oshiradi.");
        FACTS.add("Baʼzi daraxtlar minora shaklida o‘sadi.");
        FACTS.add("Kichik qadamlar kundalik hayotda katta natija beradi.");
        FACTS.add("Musika ongga taʼsir qilib kayfiyatni yaxshilashi isbotlangan.");
        FACTS.add("Toza suv manbalarini himoya qilish ekologiya uchun muhim.");
        FACTS.add("Birinchi bosqichli o‘qituvchi bolalar rivojlanishida muhim rol o‘ynaydi.");
        FACTS.add("Oldindan rejalashtirish va kichik maqsadlar samaradorlikni oshiradi.");
        FACTS.add("Kitob o‘qish tasavvurni kengaytiradi va empatiyani oshiradi.");
        FACTS.add("Baʼzi tabiiy pigmentlar terini himoya qiladi.");
        FACTS.add("Qadimgi navigatsiya yulduzlar orqali amalga oshirilgan.");
        FACTS.add("Okean sathidagi harorat iqlimga taʼsir ko‘rsatadi.");
        FACTS.add("Oddiygina meditatsiya stressni kamaytirishi mumkin.");
        FACTS.add("To‘liq 100 ta fakt shu yerda keltirildi — har safar random tanlanadi.");
    }

    public static String getRandomFact() {
        int i = R.nextInt(FACTS.size());
        return "✨ *QIZIQARLI FAKT* (№" + (i + 1) + ")\n\n" + FACTS.get(i);
    }


}
