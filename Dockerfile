# Render shu fayl bo'yicha ilovani quradi va ishga tushiradi.
# Sozlamalar (BOT_TOKEN, DB_URL, DB_USER, DB_PASSWORD, ADMIN_IDS) obrazga yozilmaydi —
# ular Render'dagi Environment bo'limida beriladi.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Avval faqat pom.xml ko'chiriladi: kod o'zgarganda kutubxonalar qayta yuklanmasin.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/dorixona-bot-1.0.0.jar app.jar
COPY --from=build /app/target/dependency ./dependency

# Render PORT ni o'zi beradi; HealthServer o'sha portni eshitib "OK" javob qaytaradi.
EXPOSE 8080
CMD ["java", "-cp", "app.jar:dependency/*", "com.company.Main"]
