import { AttributedText } from './AttributedText';
import { TravelEstimates } from './TravelEstimates';
import { ColorValue, ImageSourcePropType, ProcessedColorValue } from 'react-native';
/**
 * Navigation instructions and distance from the previous maneuver.
 */
export interface Maneuver {
    /**
     * Large image displayed below instruction
     * variants
     */
    junctionImage?: ImageSourcePropType;
    /**
     * Travel estimates are displayed on the bottom
     * container view for the currently active navigation session 
     */
    initialTravelEstimates?: TravelEstimates;
    /**
     * Image that contains a turn type signal
     * to give directional support for the user
     */
    symbolImage?: ImageSourcePropType;
    /**
     * The size of the image in points. Please read the CarPlay App Programming Guide
     * to get the recommended size.
     */
    symbolImageSize?: {
        width: number;
        height: number;
    };
    /**
     * Allows the supplied symbol image to be tinted
     * via a color, ie. 'red'. This functionality would usually
     * be available via the `<Image>` tag but carplay requires
     * an image asset to this tinting is done on the native side.
     * If a string is supplied, it will be passed to `processColor`.
     * You may also use `processColor` yourself.
     */
    tintSymbolImage?: null | number | ColorValue | ProcessedColorValue;
    /**
     * Text shown inside the carplay application
     */
    instructionVariants: string[];
     /**
     * Text shown outside the carplay application
     */
    dashboardInstructionVariants?: string[];
     /**
     * Text shown as a notification outside the carplay application
     */
    notificationInstructionVariants?: string[];
    /**
     * Attribute Text with Image support, shown inside the
     * carplay application
     */
    attributedInstructionVariants?: AttributedText[];
     /**
     * Attribute Text with Image support, shown outside the
     * carplay application
     */
    dashboardAttributedInstructionVariants?: AttributedText[];
    /**
     * Attribute Text with Image support, shown as a notification
     * outside the carplay application
     */
    notificationAttributedInstructionVariants?: AttributedText[];
}
//# sourceMappingURL=Maneuver.d.ts.map