from flask import jsonify
from flask_jwt_extended import jwt_required

import requests
import PyPDF2
from io import BytesIO


from . import api_bp


@api_bp.route('/mensa', methods=['GET'])
@jwt_required()
def mensa():
    weekdays = ["Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag"]

    # PDF-Datei von der URL herunterladen
    response = requests.get("https://www.htl-mensa.at/wp-content/uploads/2025/01/wochenplan-1.pdf")

    # Den Inhalt der PDF als BytesIO-Objekt öffnen
    with BytesIO(response.content) as file:
        # PDF-Reader erstellen
        reader = PyPDF2.PdfReader(file)
        
        # Gesamtzahl der Seiten im PDF
        
        # Text aus jeder Seite extrahieren
        text = ""
        page = reader.pages[0]
        text = page.extract_text()
        
        newtext = ""
        found = 0
        for i in range(len(text)):
            if found > 5:
                newtext = text[i:]
                break
            if text[i] == "\n":
                found += 1
                
        menu1 = ""
        
        for i in range(len(newtext)):
            if newtext[i] == "M" and newtext[i+1] == "e" and newtext[i+2] == "n" and newtext[i+3] == "ü":
                menu1 = newtext[:i]
                newtext = newtext[i:]
                break
        
        menu2 = ""
        menu3 = ""
        for i in range(len(newtext)):
            if newtext[i+1] == "M" and newtext[i+2] == "e" and newtext[i+3] == "n" and newtext[i+4] == "ü":
                menu2 = newtext[:i]
                menu3 = newtext[i:]
                break
            
        found = 0
        
        for i in range(len(menu2)):
            if menu2[i] == "\n":
                found += 1
                if found == 3:
                    menu2 = menu2[i:]
                    break
        
        found = 0 
        for i in range(len(menu3)):
            if menu3[i] == "\n":
                found += 1
                if found == 4:
                    menu3 = menu3[i:]
                    break
        
        
        found = 0
        for i in range(len(menu3)-1, 0, -1):
            if menu3[i] == "\n":
                found += 1
                if found == 3:
                    menu3 = menu3[:i]
                    break
            
        for i in range(len(menu1)):
            if menu1[i] == "\n":
                menu1 = menu1[:i] + " " + menu1[i+1:]
            
        for i in range(len(menu2)):
            if menu2[i] == "\n":
                menu2 = menu2[:i] + " " + menu2[i+1:]
                
        for i in range(len(menu3)):
            if menu3[i] == "\n":
                menu3 = menu3[:i] + " " + menu3[i+1:]


        # print(menu1)
        # print()
        # print(menu2)
        # print()
        # print(menu3)
        
        menu1_list = []
        menu2_list = []
        menu3_list = []
        
        last_i = 0
        for i in range(len(menu1)-1):
            if menu1[i] == ")":
                menu1_list.append(menu1[last_i:i+1])
                last_i = i+1
        
        last_i = 0
        for i in range(len(menu2)-1):
            if menu2[i] == ")":
                menu2_list.append(menu2[last_i:i+1])
                last_i = i+1
        
        last_i = 0
        for i in range(len(menu3)-1):
            if menu3[i] == ")":
                menu3_list.append(menu3[last_i:i+1])
                last_i = i+1
        
        for menu in menu1_list:
            if menu.startswith("  ****"):
                menu1_list[menu1_list.index(menu)] = menu[6:]
                
        for menu in menu2_list:
            if menu.startswith("  ****"):
                menu2_list[menu2_list.index(menu)] = menu[6:]
                
        for menu in menu3_list:
            if menu.startswith("  ****"):
                menu3_list[menu3_list.index(menu)] = menu[6:]
        
        soups = []
        main_dishes1 = []
        for m in menu1_list:
            if menu1_list.index(m) % 2 == 0:
                soups.append(m)
            else:
                main_dishes1.append(m)
        
        main_dishes2 = []
        for m in menu2_list:
            if menu2_list.index(m) % 2 == 1:
                main_dishes2.append(m)
        
            main_dishes3 = []
        for m in menu3_list:
            if menu3_list.index(m) % 2 == 1:
                main_dishes3.append(m)
            
        menu = {i:{'soup': '', 'dish1': '', 'dish2': '', 'dish3': ''} for i in weekdays}
        
        for day in weekdays:
            menu[day]['soup'] = soups[weekdays.index(day)]
            menu[day]['dish1'] = main_dishes1[weekdays.index(day)]
            menu[day]['dish2'] = main_dishes2[weekdays.index(day)]
            menu[day]['dish3'] = main_dishes3[weekdays.index(day)]
    
    return jsonify({'menu': menu})