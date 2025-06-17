from flask import jsonify
from flask_jwt_extended import create_access_token, get_jwt_identity, jwt_required

from . import auth_bp

@auth_bp.route('/refresh', methods=['POST'])
@jwt_required(refresh=True)
def refresh():
    current_user = get_jwt_identity()
    new_access_token = create_access_token(identity=current_user)
    return jsonify(access_token=new_access_token), 200
    