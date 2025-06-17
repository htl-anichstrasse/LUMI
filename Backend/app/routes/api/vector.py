from flask import jsonify, session
from flask_jwt_extended import jwt_required

from app.models import User
from app.models import Vector

from app.extensions import db

from . import api_bp

@api_bp.route('/vector/<int:id>', methods=['GET'])
@jwt_required()
def get_vector(id):
    vector = db.session.query(Vector).filter_by(id=id).first()

    if not vector or vector.user_id != session.get('user_id'):
        return jsonify({"error": "Vector not found"}), 404

    return jsonify(vector), 200

@api_bp.route('/vector/<int:id>', methods=['DELETE'])
@jwt_required()
def delete_vector(id):
    vector = db.session.query(Vector).filter_by(id=id).first()

    if not vector:
        return jsonify({"error": "Vector not found"}), 404

    db.session.delete(vector)
    db.session.commit()

    return jsonify({'message': 'Vector deleted'}), 200

@api_bp.route('/vectors', methods=['GET'])
@jwt_required()
def get_vectors():
    vectors = db.session.query(Vector).filter_by(user_id=session.get('user_id')).all()

    if not vectors:
        return jsonify({"error": "No vectors found for this user."}), 404

    vector_data = [{"text": v.text, "time": v.created_at} for v in vectors]

    return jsonify(vector_data), 200

@api_bp.route('/vectors', methods=['DELETE'])
@jwt_required()
def delete_vectors():
    db.session.query(Vector).filter_by(user_id=session.get('user_id')).delete()
    db.session.commit()

    return jsonify({'message': 'Vectors deleted'}), 200