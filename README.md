# 🎙️ Vocalis Studio Console

> **Vocalis Studio Console** est une application d'enregistrement audio numérique et de gestion de pistes haute fidélité développée en **JavaFX 13+** pour Linux (Debian 13 Trixie / Ubuntu). Elle offre un design Cyber-Studio sombre ultra-épuré, un visualiseur de forme d'onde en temps réel, un VU-mètre réactif et des contrôles de lecture ultra-rapides.

---

## ✨ Fonctionnalités Principales

- 🎙️ **Enregistrement Audio PCM WAV Haute Fidélité** : Capture en $44.1\,\text{kHz}$, 16-bit stéréo sans compression avec sauvegarde automatique en arrière-plan.
- 📊 **Visualiseur d'Onde & VU-Mètre Réactif** :
  - Rendu fluide sur canvas dédié du signal audio en temps réel.
  - Overlay dynamique *"⏸ EN PAUSE"* lors de la mise en pause de la capture.
  - VU-mètre réactif avec animation Peak Hold.
- 🎧 **Console d'Écoute Directe & Rack de Pistes** :
  - **Bouton Lecture/Pause (`▶` / `⏸`)** sur chaque carte d'enregistrement.
  - **Bouton d'Arrêt Net (`[ ⏹ ]`)** permanent pour interrompre instantanément l'écoute ($0\,\text{ms}$).
  - **Bouton de Suppression (`🗑`)** avec boite de confirmation sécurisée.
- 🎨 **Interface Cyber-Studio & Icônes Vectorielles** :
  - Design Glassmorphic sombre moderne (palette Tailwind Slate & Neon Accents).
  - Icônes vectorielles natifs HD via **Ikonli Feather Icons** (`org.kordamp.ikonli`).
  - Barres de défilement customisées ultra-fines (8px) pour un rendu épuré.
- 🔒 **Sécurité Audio & Exclusivité de Flux** :
  - Désactivation automatique des boutons d'écoute pendant l'enregistrement micro pour éviter tout larsen/overlap.
  - Interruption automatique de toute lecture en cours au lancement d'une nouvelle piste ou d'un enregistrement.
- 📦 **Packaging AppImage Ultra-Léger (< 10 Mo)** :
  - Binaire autonome autonome sans dépendance lourde d'installation.

---

## 🛠️ Structure du Projet (Architecture MVC & Services)

```text
recorder/
├── dist/                          # Exécutable AppImage généré
│   └── Vocalis-Studio-x86_64.AppImage
├── src/main/java/com/moafogaus/
│   ├── App.java                   # Classe principale d'application JavaFX
│   ├── MainLauncher.java          # Point d'entrée pour Fat JAR / AppImage
│   ├── controller/
│   │   └── MainController.java    # Contrôleur FXML principal (gestion UI & événements)
│   ├── model/
│   │   ├── AudioRecording.java    # Modèle de données d'un enregistrement audio
│   │   ├── AudioSettings.java     # Configuration des paramètres micro
│   │   ├── PlaybackState.java     # Énumération des états de lecture
│   │   └── RecordingState.java    # Énumération des états d'enregistrement
│   ├── service/
│   │   ├── AudioPlayerService.java # Moteur de lecture audio Java Sound (SourceDataLine)
│   │   ├── AudioRecorderService.java # Moteur de capture audio PCM & écriture WAV RIFF
│   │   ├── DeviceManager.java     # Scan et détection des microphones système (ALSA)
│   │   └── RecordingManager.java  # Gestion du dossier de stockage et parsing $O(1)$ WAV
│   ├── ui/
│   │   ├── VUMeter.java           # Composant graphique VU-Mètre customisé
│   │   └── WaveformCanvas.java    # Composant visualiseur d'ondes audio
│   └── util/
│       ├── FormatUtils.java       # Formateurs de temps et de taille fichier
│       └── IconUtils.java         # Générateur d'icônes vectorielles Feather
├── src/main/resources/com/moafogaus/
│   ├── main.fxml                  # Layout FXML de la console studio
│   ├── styles.css                 # Feuille de style CSS Cyber-Studio
│   └── icon.png                   # Icône officielle du logiciel
└── pom.xml                        # Fichier de configuration Maven & Dépendances
```

---

## 🚀 Prérequis

- **Système d'exploitation** : Linux Debian 13 (Trixie), Ubuntu 22.04+ ou toute distribution Linux x86_64.
- **Java** : OpenJDK 11, 17, 21 ou LTS 25 (Java Runtime / JRE installé).
- **Maven** : Apache Maven 3.6+ (pour la compilation à partir du code source).

---

## ⚙️ Exécution & Utilisation

### Option 1 : Exécution via l'AppImage (Recommandé - Sans Installation)

Vous pouvez lancer le binaire précompilé ultra-léger (**8,9 Mo**) :

```bash
chmod +x dist/Vocalis-Studio-x86_64.AppImage
./dist/Vocalis-Studio-x86_64.AppImage
```

---

### Option 2 : Exécution via Maven (Mode Développement)

Pour exécuter directement le projet JavaFX depuis le code source :

```bash
mvn clean javafx:run
```

---

### Option 3 : Compilation du Fat JAR & Régénération d'AppImage

Pour construire le JAR autonome et régénérer le paquet `.AppImage` :

```bash
# 1. Compilation du Fat JAR autonome
mvn clean package

# 2. Mise à jour de l'AppImage
cp target/vocalis-studio.jar dist/Vocalis-Studio.AppDir/usr/bin/vocalis-studio.jar
ARCH=x86_64 /tmp/appimagetool dist/Vocalis-Studio.AppDir dist/Vocalis-Studio-x86_64.AppImage
```

---

## 📂 Gestion des Fichiers Audio

Par défaut, Vocalis Studio enregistre vos fichiers audio au format `.wav` dans le dossier utilisateur :
`~/VocalisRecordings/`

Vous pouvez modifier le dossier de destination à tout moment directement depuis le bouton **« MODIFIER »** dans l'en-tête de l'application, ou l'ouvrir dans votre explorateur via l'icône de dossier `📂`.

---

## 📜 Licence & Crédits

- **Auteur** : MOAFOGAUS / Vocalis Team
- **Framework Graphique** : JavaFX 13
- **Bibliothèque d'Icônes** : Ikonli Feather Pack
