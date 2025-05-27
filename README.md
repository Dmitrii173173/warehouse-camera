# Warehouse Camera

## Description

Warehouse Camera is a mobile application designed to simplify the process of documenting items in a warehouse. The app allows you to:

- Create and manage goods receipts
- Enter manufacturer and article information
- Select defect categories (3 levels of importance)
- Take photos of damages and barcodes with importance markers
- Save both original and marked versions of photos
- Automatically correct photo orientation using EXIF data
- Save defect information in multilingual text files
- View taken photos through a built-in image viewer
- Manage files through a built-in file manager
- Synchronize receipt list with file system

## Design Features

The app is designed in a minimalist Apple style with the following principles:

- **Intuitive interface**: Clean, concise design focused on content
- **Smooth animations**: Button press animations, smooth transitions between screens
- **Unified design**: Consistent application of styles throughout the app
- **Improved navigation**: Back buttons on all screens for convenient movement
- **Tactile feedback**: Buttons react to presses with visual effects
- **Marking system**: Colored circles to indicate defect importance

## Technical Details

- Programming language: Kotlin
- Minimum Android version: 6.0 (API 24)
- Target Android version: 14 (API 34)
- Libraries used:
  - AndroidX Camera - for camera operations and photography
  - AndroidX RecyclerView - for displaying lists
  - Google Material Design - for modern UI
  - GSON - for JSON data processing
  - PhotoView - for viewing images with zoom capability
  - ExifInterface - for working with image metadata
  - MediaStore - for integration with device gallery

## File System Structure

The application saves photos and data in the following structure:

```
DCIM/warehouse/
  ├── {manufacturer_code}/
  │   ├── {manufacturer_code}main.txt     # Summary file with a list of all articles
  │   └── {date}/
  │       └── {defect_category}/
  │           └── {article}/
  │               ├── damage-{article}.jpg        # Original damage photo
  │               ├── damage-{article}_marked.jpg # Damage photo with color marker
  │               ├── barcode-{article}.jpg        # Original barcode photo
  │               ├── barcode-{article}_marked.jpg # Barcode photo with color marker
  │               └── {article}.txt                # Text information about the defect
```

> **Note**: For Android 10 and above (API 29+), files are saved in the app's private folder: `Android/data/com.warehouse.camera/files/Pictures/warehouse/`

## Installation

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or higher
- Gradle 8.5 or higher

### Installation Steps
1. Clone the repository:
   ```
   git clone https://github.com/Dmitrii173173/warehouse-camera.git
   ```
2. Open the project in Android Studio
3. Sync the project with Gradle
4. Run the app on an emulator or physical device

## Usage

1. On first launch, the app will request permissions to use the camera and storage
2. Create a new receipt, specifying the manufacturer code and date
3. Enter article information and select the defect category
4. Fill in defect details, choosing the reason and template
5. Take photos of damages and barcodes, selecting the criticality level (1-3)
6. Save the information
7. After completing work with the current article, return to the manufacturer information to continue working

### Additional Features

- **Photo marking system**: Color marking of photos with three importance levels (green - 1, yellow - 2, red - 3)
- **Saving originals**: The app saves both original photos and versions with markers
- **File management**: Navigation through the file system with the ability to view and delete files and folders
- **Automatic summary file creation**: Generation of a list of all articles in the manufacturer folder
- **Multilingual descriptions**: Support for Russian, English, and Chinese languages in text files
- **Barcode scanning**: Quick article entry using the camera
- **Automatic orientation correction**: Correcting photo orientation using EXIF data

## UI/UX Features

- **Card interface**: Information is presented as cards with soft rounded corners
- **Minimalist forms**: Clean and simple data entry forms with clear hints
- **Smart colors**: Use of color indicators for statuses and defect categories
- **Responsive buttons**: Buttons visually react to presses, providing feedback
- **Photo viewing**: Convenient photo viewing with zoom capability through the file manager

## New Features and Fixes

### Version 1.1.0

- **Photo orientation correction** - automatic correction of photo orientation using EXIF data
- **Deleting photos from gallery** - when deleted through the file manager, photos are also removed from the device gallery
- **Receipt list synchronization** - the receipt list is now automatically synchronized with existing folders in the file system
- **Improved photo viewing** - direct viewing of photos from the file manager with zoom capability
- **Flash fix** - improved flash operation in photo mode

## License

This project is distributed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

# Warehouse Camera

## Описание проекта

Warehouse Camera - это мобильное приложение, разработанное для упрощения процесса документирования товаров на складе. Приложение позволяет:

- Создавать и управлять приёмками товаров
- Вводить информацию о производителе и артикулах
- Выбирать категории дефектов (3 уровня важности)
- Делать фотографии повреждений и штрихкодов с маркировкой важности
- Сохранять как оригинальные, так и маркированные версии фотографий
- Автоматически исправлять ориентацию фотографий с помощью данных EXIF
- Сохранять информацию о дефектах в многоязычных текстовых файлах
- Просматривать сделанные фотографии через встроенный просмотрщик изображений
- Управлять файлами через встроенный файловый менеджер
- Синхронизировать список приёмок с файловой системой

## Особенности дизайна

Приложение выполнено в минималистичном стиле Apple с учетом следующих принципов:

- **Интуитивно понятный интерфейс**: чистый, лаконичный дизайн с фокусом на контенте
- **Плавные анимации**: анимации нажатия кнопок, плавные переходы между экранами
- **Единый дизайн**: последовательное применение стилей по всему приложению
- **Улучшенная навигация**: кнопки "Назад" на всех экранах для удобного перемещения
- **Тактильная обратная связь**: кнопки реагируют на нажатия визуальными эффектами
- **Система маркировки**: цветные кружки для обозначения важности дефектов

## Технические детали

- Язык программирования: Kotlin
- Минимальная версия Android: 6.0 (API 24)
- Целевая версия Android: 14 (API 34)
- Используемые библиотеки:
  - AndroidX Camera - для работы с камерой и фотосъемки
  - AndroidX RecyclerView - для отображения списков
  - Google Material Design - для современного UI
  - GSON - для работы с JSON данными
  - PhotoView - для просмотра изображений с возможностью масштабирования
  - ExifInterface - для работы с метаданными изображений
  - MediaStore - для интеграции с галереей устройства

## Структура файловой системы

Приложение сохраняет фотографии и данные в следующей структуре:

```
DCIM/warehouse/
  ├── {код_производителя}/
  │   ├── {код_производителя}main.txt     # Сводный файл со списком всех артикулов
  │   └── {дата}/
  │       └── {категория_дефекта}/
  │           └── {артикул}/
  │               ├── damage-{артикул}.jpg        # Оригинальное фото повреждения
  │               ├── damage-{артикул}_marked.jpg # Фото повреждения с маркером цветности
  │               ├── barcode-{артикул}.jpg        # Оригинальное фото штрихкода
  │               ├── barcode-{артикул}_marked.jpg # Фото штрихкода с маркером цветности
  │               └── {артикул}.txt                # Текстовая информация о дефекте
```

> **Примечание**: Для Android 10 и выше (API 29+), файлы сохраняются в приватной папке приложения: `Android/data/com.warehouse.camera/files/Pictures/warehouse/`

## Установка

### Требования
- Android Studio Hedgehog (2023.1.1) или новее
- JDK 17 или выше
- Gradle 8.5 или выше

### Шаги по установке
1. Клонируйте репозиторий:
   ```
   git clone https://github.com/Dmitrii173173/warehouse-camera.git
   ```
2. Откройте проект в Android Studio
3. Синхронизируйте проект с Gradle
4. Запустите приложение на эмуляторе или физическом устройстве

## Использование

1. При первом запуске приложение запросит разрешения на использование камеры и хранилища
2. Создайте новую приёмку, указав код производителя и дату
3. Введите информацию об артикуле и выберите категорию дефекта
4. Заполните детали дефекта, выбрав причину и шаблон
5. Сделайте фотографии повреждений и штрихкодов, выбрав уровень критичности (1-3)
6. Сохраните информацию
7. После завершения работы с текущим артикулом вернитесь к информации о производителе для продолжения работы

### Дополнительные функции

- **Система маркировки фотографий**: Цветовая маркировка фотографий тремя уровнями важности (зеленый - 1, желтый - 2, красный - 3)
- **Сохранение оригиналов**: Приложение сохраняет как оригинальные фотографии, так и версии с маркировкой
- **Управление файлами**: Навигация по файловой системе с возможностью просмотра, удаления файлов и папок
- **Автоматическое создание сводных файлов**: Генерация списка всех артикулов в папке производителя
- **Многоязычные описания**: Поддержка русского, английского и китайского языков в текстовых файлах
- **Сканирование штрихкодов**: Быстрый ввод артикулов с помощью камеры
- **Автоматическая коррекция ориентации**: Исправление ориентации фотографий с использованием данных EXIF

## Особенности UI/UX

- **Карточный интерфейс**: Информация представлена в виде карточек с мягкими закругленными углами
- **Минималистичные формы**: Чистые и простые формы ввода данных с понятными подсказками
- **Умные цвета**: Использование цветовых индикаторов для статусов и категорий дефектов
- **Отзывчивые кнопки**: Кнопки визуально реагируют на нажатия, обеспечивая обратную связь
- **Просмотр фотографий**: Удобный просмотр фотографий с возможностью увеличения через файловый менеджер

## Новые функции и исправления

### Версия 1.1.0

- **Исправление ориентации фотографий** - автоматическое исправление ориентации фото с использованием данных EXIF
- **Удаление фотографий из галереи** - при удалении через файловый менеджер фотографии также удаляются из галереи устройства
- **Синхронизация списка приёмок** - список приёмок теперь автоматически синхронизируется с существующими папками в файловой системе
- **Улучшенный просмотр фотографий** - прямой просмотр фотографий из файлового менеджера с возможностью масштабирования
- **Исправление вспышки** - улучшена работа вспышки в режиме фотосъемки

## Лицензия

Этот проект распространяется под лицензией MIT. Подробности смотрите в файле [LICENSE](LICENSE).

---

# Warehouse Camera

## 项目描述

Warehouse Camera 是一款设计用于简化仓库货物记录过程的移动应用。该应用允许：

- 创建和管理货物接收
- 输入制造商和物品编号信息
- 选择缺陷类别（3级重要性）
- 拍摄带有重要性标记的损坏和条形码照片
- 保存照片的原始版本和标记版本
- 使用EXIF数据自动修正照片方向
- 在多语言文本文件中保存缺陷信息
- 通过内置图像查看器查看拍摄的照片
- 通过内置文件管理器管理文件
- 将接收列表与文件系统同步

## 设计特点

该应用采用了苹果风格的极简设计，遵循以下原则：

- **直观的界面**：干净、简洁的设计，专注于内容
- **流畅的动画**：按钮按压动画，屏幕之间的平滑过渡
- **统一设计**：整个应用程序中一致应用的样式
- **改进的导航**：所有屏幕上的"返回"按钮，方便移动
- **触觉反馈**：按钮通过视觉效果对按压做出反应
- **标记系统**：彩色圆圈表示缺陷的重要性

## 技术细节

- 编程语言：Kotlin
- 最低Android版本：6.0（API 24）
- 目标Android版本：14（API 34）
- 使用的库：
  - AndroidX Camera - 用于相机操作和摄影
  - AndroidX RecyclerView - 用于显示列表
  - Google Material Design - 用于现代UI
  - GSON - 用于处理JSON数据
  - PhotoView - 用于带缩放功能的图像查看
  - ExifInterface - 用于处理图像元数据
  - MediaStore - 用于与设备图库集成

## 文件系统结构

应用程序将照片和数据保存在以下结构中：

```
DCIM/warehouse/
  ├── {制造商代码}/
  │   ├── {制造商代码}main.txt     # 包含所有物品编号列表的摘要文件
  │   └── {日期}/
  │       └── {缺陷类别}/
  │           └── {物品编号}/
  │               ├── damage-{物品编号}.jpg        # 原始损坏照片
  │               ├── damage-{物品编号}_marked.jpg # 带颜色标记的损坏照片
  │               ├── barcode-{物品编号}.jpg        # 原始条形码照片
  │               ├── barcode-{物品编号}_marked.jpg # 带颜色标记的条形码照片
  │               └── {物品编号}.txt                # 关于缺陷的文本信息
```

> **注意**：对于Android 10及以上版本（API 29+），文件保存在应用程序的私有文件夹中：`Android/data/com.warehouse.camera/files/Pictures/warehouse/`

## 安装

### 要求
- Android Studio Hedgehog（2023.1.1）或更新版本
- JDK 17或更高版本
- Gradle 8.5或更高版本

### 安装步骤
1. 克隆存储库：
   ```
   git clone https://github.com/Dmitrii173173/warehouse-camera.git
   ```
2. 在Android Studio中打开项目
3. 将项目与Gradle同步
4. 在模拟器或物理设备上运行应用程序

## 使用

1. 首次启动时，应用程序将请求使用相机和存储的权限
2. 创建新的接收单，指定制造商代码和日期
3. 输入物品编号信息并选择缺陷类别
4. 填写缺陷详情，选择原因和模板
5. 拍摄损坏和条形码的照片，选择严重程度级别（1-3）
6. 保存信息
7. 完成当前物品编号的工作后，返回制造商信息以继续工作

### 附加功能

- **照片标记系统**：使用三个重要性级别对照片进行颜色标记（绿色 - 1，黄色 - 2，红色 - 3）
- **保存原件**：应用程序保存原始照片和带标记的版本
- **文件管理**：通过文件系统导航，能够查看和删除文件和文件夹
- **自动创建摘要文件**：在制造商文件夹中生成所有物品编号的列表
- **多语言描述**：在文本文件中支持俄语、英语和中文
- **条形码扫描**：使用相机快速输入物品编号
- **自动方向校正**：使用EXIF数据校正照片方向

## UI/UX特点

- **卡片界面**：信息以带有柔和圆角的卡片形式呈现
- **极简表单**：干净简洁的数据输入表单，提示清晰
- **智能颜色**：使用颜色指示器表示状态和缺陷类别
- **响应式按钮**：按钮对按压做出视觉反应，提供反馈
- **照片查看**：通过文件管理器方便查看照片，并具有缩放功能

## 新功能和修复

### 版本1.1.0

- **照片方向校正** - 使用EXIF数据自动校正照片方向
- **从图库中删除照片** - 通过文件管理器删除时，照片也会从设备图库中删除
- **接收列表同步** - 接收列表现在会自动与文件系统中的现有文件夹同步
- **改进的照片查看** - 从文件管理器直接查看照片，具有缩放功能
- **闪光灯修复** - 改进了拍照模式下的闪光灯操作

## 许可证

本项目根据MIT许可证分发。详情请参阅[LICENSE](LICENSE)文件。