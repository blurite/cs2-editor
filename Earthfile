FROM amazoncorretto:11

WORKDIR cs2-editor

gradle:
    COPY build.gradle settings.gradle gradlew .
    COPY ./gradle ./gradle
    RUN ./gradlew build

build:
    FROM +gradle
    COPY ./src ./src

    RUN ./gradlew build

    SAVE ARTIFACT ./cs2-editor-*.jar /cs2-editor.jar
