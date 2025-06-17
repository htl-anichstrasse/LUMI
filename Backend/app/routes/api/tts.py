from flask import request, jsonify, send_file
from app.utils import openai_client
import tempfile

from . import api_bp

@api_bp.route("/tts", methods=["GET"])
def tts():
    
    text = request.args.get('text', '')
    if not text:
        return jsonify({"error": "Missing text parameter."}), 400

    try:
        # Sprache generieren mit OpenAI
        response = openai_client.audio.speech.create(
            model="tts-1",
            voice="nova",
            input=text
        )

        # Temporäre MP3-Datei speichern
        temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".mp3")
        temp_file.write(response.content)
        temp_file.flush()
        temp_file.close()

        return send_file(temp_file.name, mimetype="audio/mpeg", as_attachment=False)
    
    except Exception as e:
        return jsonify({"error": str(e)}), 500