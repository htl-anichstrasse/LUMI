from flask import request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
import datetime
from sqlalchemy import and_

from app.extensions import db

from app.models import GeoData, User

from . import api_bp

@api_bp.route('/geo', methods=['GET'])
@jwt_required()
def get_geo_data():
    if not request.args.get('from_date') or not request.args.get('to_date'):
        return jsonify({'message': 'From date and to date required'}), 400
    
    user_id = get_jwt_identity()
    
    from_date = datetime.datetime.strptime(request.args.get('from_date'), '"%Y-%m-%d %H:%M:%S"')
    to_date = datetime.datetime.strptime(request.args.get('to_date'), '"%Y-%m-%d %H:%M:%S"')
    
    
    geo_data = db.session.query(GeoData).filter_by(user_id=user_id).all()
    geo_data = [g for g in geo_data if g.created_at >= from_date and g.created_at <= to_date]
    
    if not geo_data:
        return jsonify({'message': 'No geo data found'}), 404
    
    return jsonify([{'data': g.data, 'created_at': g.created_at} for g in geo_data]), 200



@api_bp.route('/geo/<int:id>', methods=['GET'])
@jwt_required()
def get_geo(id):
    user_id = get_jwt_identity()
    geo_data = db.session.query(GeoData).filter_by(and_(id=id, user_id=user_id)).first()
    if not geo_data:
        return jsonify({'message': 'Geo data not found'}), 404
    return jsonify({'data': geo_data.data, 'created_at':geo_data.created_at}), 200


@api_bp.route('/geo', methods=['POST'])
@jwt_required()
def post_geo():
    user_id = get_jwt_identity()
    data = request.get_json().get('data')
    if not data:
        return jsonify({'message': 'Data required'}), 400
    
    geo_data = GeoData(data, user_id)
    db.session.add(geo_data)
    db.session.commit()
    return jsonify({'message': 'Geo data saved'}), 201

@api_bp.route('/geo/<int:id>', methods=['DELETE'])
@jwt_required()
def delete_geo(id):
    user_id = get_jwt_identity()
    geo_data = db.session.query(GeoData).filter_by(and_(id=id, user_id=user_id)).first()
    if not geo_data:
        return jsonify({'message': 'Geo data not found'}), 404
        
    db.session.delete(geo_data)
    db.session.commit()
    return jsonify({'message': 'Geo data deleted'})