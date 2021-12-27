# Ori-Project ref: [Github repo](https://github.com/Kuhaneswaran/Text-Recognition?ref=androidexample365.com)


<img src="https://user-images.githubusercontent.com/41557821/135852968-6fe6e572-0246-4623-a7ea-50dfe5b47947.jpg" alt="android" width="300"/>

# Current issue: 
- Inconsistent bounding box detection, the text extraction is based on the number of bounding box, so this will be an issue. 

# Possible solution/ Todo: 
*Remark: The logic for this part is under MLKitUtils.kt, can go to this file to make changes. 
1. Use Regex to extract IC number 
2. Get the Ic number's bounding box number, usually the name bounding is `Ic number's bounding box number + 1`, this requires more testing. If this works, then we should be able to extract two info needed and solve the current issue. 

# Steps 
1. Run the app, select either from camera or from phone gallery. 
2. Scan IC, bounding boxes will be displayed. 
3. Press the Download button, and check the Run console tab on Android Studion, the extracted text should be displayed. 

