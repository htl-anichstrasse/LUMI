from flask import jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity

from app.models import User

from . import auth_bp

@auth_bp.route('/get-user', methods=['GET'])
@jwt_required()
def get_user():
    user_id = get_jwt_identity()
    print(user_id)
    user = User.query.filter_by(id=user_id).first()
        
    if not user:
        return jsonify({'message': 'User not found'}), 404
        
    return jsonify({'firstname': user.firstname, 'lastname': user.lastname, 'img': user.img, 'username': user.username, 'email': user.email, 'created_at': user.created_at}), 200