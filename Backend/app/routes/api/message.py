from flask import jsonify, request
from flask_jwt_extended import jwt_required, get_jwt_identity
import datetime
import uuid

from app.extensions import db
from app.utils import nlp_german, openai_client

from app.models import Vector
from app.models import CostTracking
from app.models import Model

from app.extensions import user_collection

from . import api_bp

@api_bp.route('/message', methods=['POST'])
@jwt_required()
def message():
    user_id = get_jwt_identity()
    data = request.get_json()
    message_type = data.get('type')
    message_text = data.get('content')
    message_title = data.get('title')
    message_time = data.get('postTime')
    message_package = data.get('package')
    
    message_text_e = data.get('encrypted_content')
    message_title_e = data.get('encrypted_title')
    
    if not all([message_type, message_text, message_title, message_time, message_package, message_text_e, message_title_e]):
        return jsonify({"error": "All fields are required"}), 400
    
    if message_type == 'message': # Anpassung bei mehreren Sätzen
        message_time = datetime.datetime.fromtimestamp(message_time)
        keywords = extract_keywords(message_title + ': ' + message_text)
        response = openai_client.embeddings.create(
            input=" ".join(keywords),
            model="text-embedding-3-small"
            )
        
        user_collection.add(
            documents=[message_title_e + ': ' + message_text_e],
            metadatas=[{"user_id": user_id, "time": str(message_time), "package": message_package}],
            ids=[str(uuid.uuid4())],
            embeddings=[response.data[0].embedding]
        )
        
        tokens = len(''.join(keywords))/4
        model = db.session.query(Model).filter_by(name="text-embedding-3-small").first()
        db.session.add(CostTracking(tokens, "input", model.id, user_id))
        
        db.session.commit()
        
        return jsonify({'keywords': keywords, 'message': 'Notification saved'}), 201
    
    return jsonify({'message': 'Invalid type'}), 400


def extract_keywords(text):
    doc = nlp_german(text)
    
    keywords = [token.text for token in doc if token.is_stop != True and token.is_punct != True]
    
    return keywords