from flask import request, jsonify
import requests
from deep_translator import GoogleTranslator
import ollama

from . import api_bp


def translator(text, target_lang):
    return GoogleTranslator(source='auto', target=target_lang).translate(text)

@api_bp.route("/generate_response", methods=["GET"])
def generate_response():
    question = request.args.get('question', '')
    context = request.args.get('context', '')
    if not question or not context:
        return jsonify({"error": "Missing question or context parameter."}), 400
    
    question = translator(question, 'en')
    context = translator(context, 'en')
    
    answer = requests.get(f"http://10.10.11.11/api/generate_answer?question={question}&context={context}")
    if answer.status_code == 200:
        answer_data = answer.json()['answer']
        return jsonify({'answer': translator(answer_data, 'de'), 'application': 'chat'}), 200
    else:
        return jsonify({"error": "Failed to generate answer."}), 500
