## **Dimens Generator Plugin for Android Studio**

**Description**:  
This plugin automates the task of generating dimension resources for Android applications. Instead of manually creating different `dimens.xml` files for various screen widths (like `sw600dp`, `sw720dp`), the plugin generates these files based on multipliers provided by the user.

**Features**:
- **Automated Generation** of dimension resources based on base `dimens.xml`.
- **Customizable Multipliers**: Define custom multipliers for different screen widths.
- **Backup Option**: Before modifying, creates a backup of the original layout file.
- **Interactive UI**: Provides an intuitive UI to select which dimensions to generate and what multipliers to use.

**How to Use**:
1. Open an XML layout file in Android Studio.
2. Right-click anywhere in the editor area.
3. Choose the **Generate Dimens** option from the context menu.
4. Adjust the settings as needed in the pop-up dialog and click **Generate**.
