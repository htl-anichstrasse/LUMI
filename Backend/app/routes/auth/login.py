from flask import request, jsonify
from werkzeug.security import check_password_hash

from flask_jwt_extended import create_access_token, create_refresh_token
from . import auth_bp
from app.models import User


@auth_bp.route('/login', methods=['POST'])
def login():
    data = request.get_json()
    email = data.get('email')
    password = data.get('password')
    
    if not email or not password:
        return jsonify({'message': 'Email and password are required'}), 400
    
    user = User.query.filter_by(email=email).first()
    if not user or not check_password_hash(user.password, password) == True:
        return jsonify({'message': 'Invalid email or password'}), 401
    access_token = create_access_token(identity=str(user.id))
    refresh_token = create_refresh_token(identity=str(user.id))
    return jsonify({'message': 'Login successful', 'access_token': access_token, 'refresh_token': refresh_token}), 200