ALTER TABLE conversation_messages ALTER COLUMN channel_message_id TYPE VARCHAR(255);
ALTER TABLE conversation_messages ALTER COLUMN reply_to_message_id TYPE VARCHAR(255);
ALTER TABLE conversation_messages ALTER COLUMN sender_phone TYPE VARCHAR(50);
ALTER TABLE conversation_messages ALTER COLUMN sender_name TYPE VARCHAR(255);
