from flask import jsonify, request, session
from werkzeug.security import generate_password_hash
import re

from flask_jwt_extended import create_access_token, create_refresh_token

from . import auth_bp

from app.models import User
from app.extensions import db

name_regex = r'^[A-Za-z]{1,50}$'  # Nur Buchstaben, maximal 50 Zeichen
email_regex = r'^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$'  # Standard-Email-Regex
password_regex = r'^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$'  # Mindestens 8 Zeichen, 1 Buchstabe, 1 Zahl
username_regex = r'^[A-Za-z0-9]{1,50}$'  # Nur Buchstaben und Zahlen, maximal 50 Zeichen


@auth_bp.route('/register', methods=['POST'])
def register():
    data = request.get_json()
    u = User(data.get('firstname'), data.get('lastname'), data.get('username'), data.get('email'), data.get('password'))
    if not re.match(name_regex, u.firstname):
        return jsonify({'message': 'Invalid first name. Only letters are allowed and max 50 characters.'}), 400

    if not re.match(name_regex, u.lastname):
        return jsonify({'message': 'Invalid last name. Only letters are allowed and max 50 characters.'}), 400

    if not re.match(email_regex, u.email):
        return jsonify({'message': 'Invalid email address.'}), 400

    if not re.match(password_regex, u.password):
        return jsonify({'message': 'Password must be at least 8 characters long and contain both letters and numbers.'}), 400
    
    if not re.match(username_regex, u.username):
        return jsonify({'message': 'Invalid username. Only letters and numbers are allowed and max 50 characters.'}), 400
    
    if User.query.filter_by(email=u.email).first():
        return jsonify({'message': 'Email already exists'}), 400
    
    img = data.get('img')
    
    if img:
        u.img = img
    
    u.password = generate_password_hash(u.password, method='pbkdf2:sha256')
    
    db.session.add(u)
    db.session.commit()
    
    u = User.query.filter_by(email=u.email).first()
    
    access_token = create_access_token(identity=str(u.id))
    refresh_token = create_refresh_token(identity=str(u.id))
    
    return jsonify({'message': 'User registered successfully.', 'access_token': access_token, 'refresh_token': refresh_token}), 201