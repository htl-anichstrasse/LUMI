from flask import request, jsonify, Response
import json
import numpy as np
import requests
from transformers import pipeline
from deep_translator import GoogleTranslator
import re
import dateparser
import openai
from dateutil.parser import parse
from datetime import datetime, timedelta
from typing import Tuple
from flask_jwt_extended import jwt_required, get_jwt_identity

from . import api_bp
from app.utils import nlp_multi
from app.api_keys import open_weather_map_key, openai_key, vao_key

from app.models import CostTracking, StopLocation, User, Vector
from app.models import Model
from app.extensions import db, user_collection

from app.models import GeoData

openai.api_key = openai_key

def get_locations(text):
    doc = nlp_multi(text)

    startort, zielort = None, None

    for token in doc:
        if token.text.lower() == "von" and token.i + 1 < len(doc):
            nächstes_token = doc[token.i + 1]
            startort = nächstes_token.text

        if token.text.lower() == "nach" and token.i + 1 < len(doc):
            nächstes_token = doc[token.i + 1]
            zielort = nächstes_token.text

        if token.text.lower() == "ziel" and token.i + 1 < len(doc) and doc[token.i + 1].text.lower() == "ist":
            if token.i + 2 < len(doc):
                zielort = doc[token.i + 2].text

    if startort or zielort:
        return startort, zielort
    return None

def get_location_history(from_date, to_date):
    geo_data = db.session.query(GeoData).filter_by(user_id=1).all()
    geo_data = [g for g in geo_data if g.created_at >= from_date and g.created_at <= to_date]
    
    if not geo_data:
        return {"application": "location history", "message": "No geo data found"}, 404
    
    return {"application": "location history", "data": [{'data': g.data, 'created_at': g.created_at} for g in geo_data]}

def extract_query_dates(text: str, model_name: str = "dslim/bert-base-NER") -> Tuple[datetime, datetime]:

    def resolve_relative_dates(text: str) -> str:
        today = datetime.now()
        weekdays = {
            "montag": 0, "dienstag": 1, "mittwoch": 2, "donnerstag": 3, "freitag": 4, "samstag": 5, "sonntag": 6
        }

        # Ersetzungen für relative Bezeichnungen
        text = re.sub(r'gestern', (today - timedelta(days=1)).strftime("%d.%m.%Y"), text, flags=re.IGNORECASE)
        text = re.sub(r'heute', today.strftime("%d.%m.%Y"), text, flags=re.IGNORECASE)

        # Relative Wochentage (z. B. "letzten Dienstag")
        for weekday, weekday_num in weekdays.items():
            match = re.search(fr'letzten\s*{weekday}', text, re.IGNORECASE)
            if match:
                days_ago = (today.weekday() - weekday_num) % 7 or 7
                text = re.sub(match.group(), (today - timedelta(days=days_ago)).strftime("%d.%m.%Y"), text)

            match = re.search(fr'nächsten\s*{weekday}', text, re.IGNORECASE)
            if match:
                days_ahead = (weekday_num - today.weekday()) % 7 or 7
                text = re.sub(match.group(), (today + timedelta(days=days_ahead)).strftime("%d.%m.%Y"), text)

        return text

    # Relative Angaben auflösen
    text = resolve_relative_dates(text)

    # NER-Pipeline initialisieren
    ner_pipeline = pipeline("ner", model=model_name, aggregation_strategy="simple")

    # Liste für erkannte Datumsangaben
    datetimes = []

    # Ergebnisse der NER-Pipeline auf den Text anwenden
    ner_results = ner_pipeline(text)

    for entity in ner_results:
        if entity['entity_group'] == 'DATE':
            try:
                # Versuchen, das erkannte Datum zu parsen
                dt = parse(entity['word'], fuzzy=True, dayfirst=True)
                datetimes.append(dt)
            except ValueError:
                continue

    # Fallback: Manuelle Suche nach Datumsangaben, falls keine gefunden wurden
    if not datetimes:
        words = text.split()
        for word in words:
            try:
                dt = parse(word, fuzzy=True, dayfirst=True)
                datetimes.append(dt)
            except ValueError:
                continue

    # Zusätzliche Verarbeitung für Reichweiten ("bis ...")
    if "bis" in text:
        parts = text.split("bis")
        if len(parts) == 2:
            try:
                start_dt = parse(parts[0], fuzzy=True, dayfirst=True)
                end_dt = parse(parts[1], fuzzy=True, dayfirst=True)
                datetimes.append(start_dt)
                datetimes.append(end_dt)
            except ValueError:
                pass

    # Sortiere die gefundenen Datumsangaben
    datetimes = sorted(datetimes)

    if len(datetimes) == 0:
        raise ValueError("No valid date found in the text.")
    elif len(datetimes) == 1:
        # Wenn nur ein Datum gefunden wurde, wird das Enddatum auf dasselbe gesetzt
        start_date = datetimes[0]
        end_date = start_date.replace(hour=23, minute=59, second=59)
        return start_date, end_date
    else:
        # Falls mehrere Datumsangaben gefunden wurden, prüfe die Reihenfolge
        start_date, end_date = datetimes[0], datetimes[-1]

        # Falls sie nicht korrekt angeordnet sind, tausche sie
        if start_date > end_date:
            start_date, end_date = end_date, start_date

        # Setze die Zeit des Enddatums auf 23:59:59
        end_date = end_date.replace(hour=23, minute=59, second=59)
        return start_date, end_date

# extract dates KI-Server:
# def extract_query_dates(text):
#     try:
#         # Definiere die URL des Servers
#         url = "http://10.10.11.11/api/extract_dates"  # Stelle sicher, dass die IP und der Port korrekt sind

#         # Sende die POST-Anfrage mit dem Text als JSON-Daten
#         response = requests.post(url, json={"text": text})

#         # Überprüfe den Statuscode der Antwort
#         if response.status_code == 200:
#             data = response.json()  # Antwort als JSON-Daten parsen
#             start_date = datetime.strptime(data["start_date"], "%Y-%m-%d %H:%M:%S")
#             end_date = datetime.strptime(data["end_date"], "%Y-%m-%d %H:%M:%S")
#             return start_date, end_date
#         else:
#             # Falls der Statuscode nicht 200 ist, eine Fehlermeldung ausgeben
#             raise ValueError(f"Server error: {response.status_code}, {response.text}")
#     except Exception as e:
#         # Bei Fehlern die Fehlermeldung ausgeben
#         raise ValueError(f"Failed to extract dates: {str(e)}")

def chatbot_response(conversation, longitude=None, latitude=None):
    try:
        # Die neue Nachricht des Nutzers extrahieren
        new_message = conversation[-1]["content"]
        original_message = new_message
        new_message = translator(new_message, 'en')

        # Intent filtern
        intent = intent_filter(new_message)

        if intent == "internet question":
            # Füge die Systemnachricht für das Chatverhalten hinzu
            system_message = {
                "role": "system",
                "content": "Respond with concise, complete answers that are no longer than two sentences. Ensure each response is self-contained and does not cut off mid-sentence."
            }
            conversation.insert(0, system_message)
            
            user_id = get_jwt_identity()
            request_tokens = len(''.join(msg["content"] for msg in conversation)) / 4
            model = db.session.query(Model).filter_by(name="GPT-4o mini").first()
            db.session.add(CostTracking(request_tokens, "input", model.id, user_id))

            # Sende die Unterhaltung an die OpenAI-API
            response = openai.chat.completions.create(
                model="gpt-4o-2024-11-20",
                messages=conversation
            )
            
            # Kosten für die Antwort tracken
            # Extrahieren der Inhalte der Antwort korrekt, ohne Indexierung
            response_choices = response.choices
            tokens = sum(len(choice.message.content) for choice in response_choices) / 4
            model = db.session.query(Model).filter_by(name="GPT-4o mini").first()
            db.session.add(CostTracking(tokens, "output", model.id, user_id))
            db.session.commit()

            # Antwort extrahieren und zurückgeben
            if response_choices:
                # Extrahieren des Inhalts aus der Wahl
                content = response_choices[0].message.content.strip()
                
                return {
                    "application": "chat",
                    "role": "system",
                    "content": content
                }
            else:
                raise ValueError("No valid response in 'choices'.")

        else:
            # Wenn der Intent nicht "internet question" ist, rufe den Service auf
            service_response = call_service(intent, new_message, longitude, latitude, original_message)
            return service_response

    except Exception as e:
        # Fehler abfangen und zurückgeben
        return {"application": "chat", "error": f"Chatbot error: {e}"}
    
def getStop(name, longitude, latitude):
    stop = None
    if name != None:
        stop = StopLocation.query.filter(StopLocation.name.like('%' + name + '%')).first()
    
    if stop:
        return stop
    
    if name:
        return getStopByName(name)
    else:
        return getStopByCoordinates(longitude, latitude)

def getStopByCoordinates(longitude, latitude):
    url = f'https://routenplaner.verkehrsauskunft.at/vao/restproxy/v1.9.0/location.name?input=*&accessId={vao_key}&format=json&coordLong={longitude}&coordLat={latitude}'
    response = requests.get(url)
    data = response.json()['stopLocationOrCoordLocation']

    stops = []

    for stop in data:
        id = stop['StopLocation']['extId']
        if id.startswith('9'):
            continue
        name = stop['StopLocation']['name']
        stops.append({'extId': id, 'name': name})
        if not StopLocation.query.filter_by(extId=id).first():
            db.session.add(StopLocation(name, id))
    
    db.session.commit()
    return StopLocation(stops[0]['name'], stops[0]['extId'])

def getStopByName(name):
    url = f'https://routenplaner.verkehrsauskunft.at/vao/restproxy/v1.9.0/location.name?input={name}&accessId={vao_key}&format=json'
    response = requests.get(url)
    data = response.json()['stopLocationOrCoordLocation']

    stops = []

    for stop in data:
        id = stop['StopLocation']['extId']
        if id.startswith('9'):
            continue
        name = stop['StopLocation']['name']
        stops.append({'extId': id, 'name': name})
        db.session.add(StopLocation(name, id))
    
    db.session.commit()
    return StopLocation(stops[0]['name'], stops[0]['extId'])

def getTrips(from_loc, to_loc):
    url = f'https://routenplaner.verkehrsauskunft.at/vao/restproxy/v1.9.0/trip?accessId={vao_key}&format=json&originId={from_loc}&destId={to_loc}'
    response = requests.get(url)

    data = response.json()['Trip']


    formated_trips = {'trips': [], 'application': 'transport'}

    for trip in data:
        origin = trip['Origin']['name']
        destination = trip['Destination']['name']
        try:
            total_departure_time = trip['Origin']['rtTime']
        except KeyError:
            try:
                total_departure_time = trip['Origin']['time']
            except KeyError:
                total_departure_time = 'N/A'

        try:
            total_arrival_time = trip['Destination']['rtTime']
        except KeyError:
            try:
                total_arrival_time = trip['Destination']['time']
            except KeyError:
                total_arrival_time = 'N/A'
        
        total_travel_time = datetime.strptime(total_arrival_time, '%H:%M:%S') - datetime.strptime(total_departure_time, '%H:%M:%S')
        
        products = trip['LegList']['Leg']
        formated_products = []
        for product in products:
            origin_date = trip['Origin']['date']
            departure_date = trip['Origin']['date']
            
            from_loc = product['Origin']['name']
            to_loc = product['Destination']['name']
            
            line = product['name']
            
            try:
                direction = product['direction']
            except KeyError:
                direction = 'N/A'
            
            try:
                departure_time = product['Origin']['rtTime']
            except KeyError:
                try:
                    departure_time = product['Origin']['time']
                except KeyError:
                    departure_time = 'N/A'
                
            
            try:
                arrival_time = product['Destination']['rtTime']
            except KeyError:
                try:
                    arrival_time = product['Destination']['time']
                except KeyError:
                    arrival_time = 'N/A'

            
            try:
                origin_track = product['Origin']['rtTrack']
            except KeyError:
                try:
                    origin_track = product['Origin']['track']
                except KeyError:
                    origin_track = 'N/A'
            
            try:
                destination_track = product['Destination']['rtTrack']
            except KeyError:
                try:
                    destination_track = product['Destination']['track']
                except KeyError:
                    destination_track = 'N/A'
                    
            if not arrival_time == 'N/A' and not departure_time == 'N/A':
                travel_time = (datetime.strptime(f"{origin_date},{arrival_time}", '%Y-%m-%d,%H:%M:%S') - datetime.strptime(f"{departure_date},{departure_time}", '%Y-%m-%d,%H:%M:%S'))
            
            if not arrival_time == 'N/A':
                delay = (datetime.strptime(arrival_time, '%H:%M:%S') - datetime.strptime(product['Destination']['time'], '%H:%M:%S')).seconds//60
            
            formated_products.append({
                'from': f"{from_loc}",
                'to': f"{to_loc}",
                'line': f"{line}",
                'direction': f"{direction}",
                'departure_time': f"{departure_time}",
                'arrival_time': f"{arrival_time}",
                'origin_track': f"{origin_track}",
                'destination_track': f"{destination_track}",
                'delay': f"{delay}",
                'travel_time': f"{travel_time.seconds//3600}:{travel_time.seconds%3600//60}"
            })
        
        formated_trips['trips'].append({
            'origin': f"{origin}",
            'destination': f"{destination}",
            'departure_time': f"{total_departure_time}",
            'arrival_time': f"{total_arrival_time}",
            'travel_time': f"{total_travel_time.seconds//3600}:{total_travel_time.seconds%3600//60}",
            'products': formated_products
        })
    
    return formated_trips
    
# Extract_Name KI-Server:
# def extract_name(text):
#     response = requests.get("http://10.10.11.11/api/name/" + text)
#     return response.json()["name"]

def extract_name(text):
    context = translator(text, 'en')
    question_anwerer = pipeline("question-answering", model="bert-large-uncased-whole-word-masking-finetuned-squad")
    answer = question_anwerer(question="Who do I need to call?", context=context)
    name =  translator(answer['answer'], 'de')
    return name

def extract_datetime(text):
    doc = nlp_multi(text)

    date_str = None
    time_str = None

    for ent in doc.ents:
        if ent.label_ == "TIME":
            time_str = ent.text
        elif ent.label_ == "DATE":
            date_str = ent.text

    if "morgen" in text.lower():
        date_str = "morgen"
    elif "übermorgen" in text.lower():
        date_str = "übermorgen"

    time_pattern = re.search(r'(\d{1,2}):(\d{2})|(\d{1,2})\s*uhr|(\d{1,2})\.(\d{2})\s*uhr', text, re.IGNORECASE)
    if time_pattern:
        if time_pattern.group(1) and time_pattern.group(2):
            time_str = f"{time_pattern.group(1)}:{time_pattern.group(2)}"
        elif time_pattern.group(3):
            time_str = f"{time_pattern.group(3)}:00"
        elif time_pattern.group(4) and time_pattern.group(5):
            time_str = f"{time_pattern.group(4)}:{time_pattern.group(5)}"

    if date_str is None:
        date_pattern = re.search(r'(\d{1,2}\.\d{1,2}\.\d{4})', text)
        if date_pattern:
            date_str = date_pattern.group(1)

    datetime_str = ""
    if date_str:
        datetime_str += date_str + " "
    if time_str:
        datetime_str += time_str

    parsed_datetime = dateparser.parse(datetime_str, languages=['de'])
    return parsed_datetime

def call(name):
    call_data = {
        "application": "call",
        "name": name
    }
    return call_data

def set_alarm(time):
    formatted_time = time.strftime("%Y-%m-%dT%H:%M:%SZ")
    alarm_data = {
        "application": "alarm",
        "time": formatted_time
    }
    return alarm_data

def get_weather_city(city):
    url = f"https://api.openweathermap.org/data/2.5/weather?q={city}&appid={open_weather_map_key}"
    url_forecast = f"https://api.openweathermap.org/data/2.5/forecast?q={city}&cnt=8&appid={open_weather_map_key}"

    response = requests.get(url)
    response_forecast = requests.get(url_forecast)

    if response.status_code == 200 and response_forecast.status_code == 200:
        data_current = response.json()
        data_forecast = response_forecast.json()

        weather_data = {
            "application": "weather",
            "current_weather": data_current,
            "forecast": data_forecast
        }

        return weather_data

def get_weather_coord(lat, lon):
    url = f"https://api.openweathermap.org/data/2.5/weather?lat={lat}&lon={lon}&cnt=1&lang=de&appid={open_weather_map_key}"
    url_forecast = f"https://api.openweathermap.org/data/2.5/forecast?lat={lat}&lon={lon}&cnt=8&appid={open_weather_map_key}"

    response = requests.get(url)
    response_forecast = requests.get(url_forecast)

    if response.status_code == 200 and response_forecast.status_code == 200:
        data_current = response.json()
        data_forecast = response_forecast.json()

        weather_data = {
            "application": "weather",
            "current_weather": data_current,
            "forecast": data_forecast
        }

        return weather_data

def call_service(intent, text, longitude, latitude, original_text):
    doc = nlp_multi(text)
    
    # Wetter-Intent
    if intent == "weather question":
        city = None
        for ent in doc.ents:
            if ent.label_ == "LOC":
                city = ent.text
                break  # Stop after finding the first location entity
        if city:
            return get_weather_city(city)
        else:
            return get_weather_coord(latitude, longitude)

    # Alarm-Intent
    elif intent == "schedule alarm":
        time = extract_datetime(text)
        if time:
            return set_alarm(time)
        else:
            return {"application": "alarm", "error": "No time specified"}
    
    # Anruf-Intent
    elif intent == "phone call":
        name = extract_name(text)
        if name != "No name found":
            return call(name)
        else:
            return {"application": "call", "error": "No name found"}
    
    # Internet-Frage-Intent
    elif intent == "internet question":
        # Starte neuen Chat
        conversation = [
            {"role": "user", "content": text}
        ]
        return chatbot_response(conversation, longitude, latitude)
    
    # Standortfrage-Intent
    elif intent == "location history question":
        from_date, to_date = extract_query_dates(text)
        location_history_response = get_location_history(from_date, to_date)
        return location_history_response
    
    elif intent == "public transport question":
        start, destination = get_locations(text)
        start_stop = getStop(start, longitude, latitude)
        dest_stop = getStop(destination, longitude, latitude)
        return getTrips(start_stop.extId, dest_stop.extId)
        
    elif intent == "personal question":
        vector = get_vector(original_text)
        vector = vector if vector else "No vector found"
        return {"application": "personal", "vector": vector, "question": original_text}

    # Intent wurde nicht erkannt
    else:
        return {"application": "None", "error": "Intent not recognized"}

def get_vector(text):
    result = user_collection.query(
        query_texts=[text],
        n_results=1,
        include=["documents", "metadatas"],
        where={"user_id": get_jwt_identity()}
    )
    final_result = result["documents"][0][0]
    print(final_result)
    return final_result
        

def translator(text, target_lang):
    return GoogleTranslator(source='auto', target=target_lang).translate(text)

# Intent-Filter KI-Server:
# def intent_filter(text):
#     response = requests.get("http://10.10.11.11/api/label/" + text)
#     return response.json()["label"]

# Intent_Filter lokal:
def intent_filter(text):
    classifier = pipeline(model="facebook/bart-large-mnli")

    intentclassifier = classifier(
        text,
        candidate_labels=[
            "internet question", "personal question", "weather question",
            "set alarm", "phone call", "location history question", "public transport question"
        ],
    )

    # Finde den höchsten Score und prüfe, ob er die Schwelle überschreitet
    max_score = max(intentclassifier["scores"])
    max_label = intentclassifier["labels"][intentclassifier["scores"].index(max_score)]

    # Wenn der Score unter 0.45 liegt, setze den Intent auf "internet question"
    # if max_score < 0.45:
    #     intent = "internet question"
    # else:
    #     intent = max_label

    # return intent
    print(f"Intent: {max_label}, Score: {max_score}")
    return max_label

@api_bp.route("/query", methods=["GET"])
@jwt_required()
def query():
    message = request.args.get("message")
    
    if not message:
        return jsonify({"error": "Missing message content"}), 400

    try:
        # Parse the message JSON
        message_data = json.loads(message)
        
        # Check if "role" exists in the message
        if isinstance(message_data, list) and all("role" in entry for entry in message_data):
            # Call chatbot_response if "role" exists
            response = chatbot_response(message_data, request.args.get("longitude"), request.args.get("latitude"))
            return jsonify(response)
        
        # If "role" doesn't exist, proceed with existing logic
        text = message_data[0]["content"]
    except (json.JSONDecodeError, KeyError):
        return jsonify({"error": "Invalid message format"}), 400
    
    # Existing logic for handling message
    translated_text = translator(text, 'en')
    print(f"Translated text: {translated_text}")
    
    intent = intent_filter(translated_text)
    
    result = call_service(intent, text, request.args.get("longitude"), request.args.get("latitude"), original_text=text)
    
    # Ensure result is JSON serializable
    if isinstance(result, tuple):
        response = result
        if isinstance(response, Response):
            result = response.get_json()
        else:
            result = response
    elif isinstance(result, Response):
        result = result.get_json()
    elif result is None:
        result = {"error": "No result returned"}
    
    return jsonify(result), 200