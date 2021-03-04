package wooga.gradle.appcenter.error

import groovy.transform.InheritConstructors

@InheritConstructors
class AppCenterUploadException extends Exception {
}

@InheritConstructors
class AppCenterAppExtractionException extends Exception {
}

@InheritConstructors
class AppCenterAppUploadServerErrorException extends Exception {
}

@InheritConstructors
class AppCenterMalwareDetectionException extends Exception {

}


