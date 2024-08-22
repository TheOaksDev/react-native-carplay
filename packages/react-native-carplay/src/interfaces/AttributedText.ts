import { ImageSourcePropType } from 'react-native';

export interface AttributedText {
    text: string;
    attributedImage?: ImageSourcePropType;
    attributedImageSize?: {
        width: number;
        height: number;
    };
  }
  