plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src"))
        }
    }
}