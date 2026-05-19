    private static byte[] convertFloat32ToShort16(byte[] inputBytes) {
        ByteBuffer inputBuffer = ByteBuffer.wrap(inputBytes).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer outputBuffer = ByteBuffer.allocate(inputBytes.length / 2).order(ByteOrder.LITTLE_ENDIAN);

        
        float volumeMultiplier = 1.78f;

        while (inputBuffer.hasRemaining()) {
            
            float sample = inputBuffer.getFloat() * volumeMultiplier;
            
          
            if (sample > 1.0f) sample = 1.0f;
            if (sample < -1.0f) sample = -1.0f;

            
            short shortSample = (short) (sample * 32767);
            outputBuffer.putShort(shortSample);
        }

        return outputBuffer.array();
    }
