from flask import jsonify

from flask_jwt_extended import jwt_required, get_jwt

from . import auth_bp
from app.extensions import blacklist



@auth_bp.route('/logout', methods=['POST'])
@jwt_required()
def logout():
    jti = get_jwt()["jti"]  # JTI (JWT ID) ist eine eindeutige ID des Tokens
    blacklist.add(jti)  # Füge das Token zur Blacklist hinzu
    return jsonify(msg="Successfully logged out"), 200