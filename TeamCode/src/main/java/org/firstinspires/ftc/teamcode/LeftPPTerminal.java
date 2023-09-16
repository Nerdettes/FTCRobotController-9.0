package org.firstinspires.ftc.teamcode;

import static org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.DEGREES;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
//test1
import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
//test
import java.util.List;
import java.util.Locale;

@Disabled
@Autonomous(name = "LeftPPTerminal", group = "")
public class LeftPPTerminal extends LinearOpMode {
    //test1
    private DcMotor LF = null;
    private DcMotor RF = null;
    private DcMotor LB = null;
    private DcMotor RB = null;
    private Servo gripper = null; //Located on Expansion Hub- Servo port 0
    private DcMotor arm = null;

    static final float MAX_SPEED = 1.0f;
    static final float MIN_SPEED = 0.4f;
    static final int ACCEL = 75;  // Scaling factor used in accel / decel code.  Was 100!
    public float desiredHeading;

    private PIDController pidRotate;
    private  OpenCvCamera webCam;
    private boolean isCameraStreaming = false;
    Pipeline modifyPipeline = new Pipeline();

    BNO055IMU imu;
    Orientation angles;
    Acceleration gravity;

    /*private static final String TFOD_MODEL_ASSET = "PowerPlay.tflite";
    private static final String[] LABELS = {
            "Bolt",
            "Bulbs",
            "Panels"
    };
    private static final String VUFORIA_KEY =
            "AVXWcGz/////AAABmZfYj2wlVElmo2nUkerrNGhEBBg+g8Gq1KY3/lN0SEBYx7HyMslyrHttOZoGtwRt7db9nfvCiG0TBEp7V/+hojHXCorf1CEvmJWWka9nFfAbOuyl1tU/IwdgHIvSuW6rbJY2UmMWXfjryO3t9nNtRqX004LcE8O2zkKdBTw0xdqq4dr9zeA9gX0uayps7t0TRmiToWRjGUs9tQB3BDmSinXxEnElq+z3SMJGcn5Aj44iEB7uy/wuB8cGCR6GfOpDRYqn/R8wwD757NucR5LXA48rulTdthGIuHoEjud1QzyQOv4BpaODj9Oi0TMuBmBzhFJMwWzyZ4lKVyOCbf3uCRia7Q+HO+LbFbghNIGIIzZC";
    private VuforiaLocalizer vuforia;
    private TFObjectDetector tfod;
*/
    private int resultROI=2;

    private  boolean done = false;


    @Override
    public void runOpMode() throws InterruptedException {

        LF = hardwareMap.get(DcMotor.class, "LF");
        RF = hardwareMap.get(DcMotor.class, "RF");
        LB = hardwareMap.get(DcMotor.class, "LB");
        RB = hardwareMap.get(DcMotor.class, "RB");

        arm = hardwareMap.get(DcMotor.class, "arm");
        gripper = hardwareMap.get(Servo.class, "gripper");

        LF.setDirection(DcMotor.Direction.REVERSE);  // motor direction set for mecanum wheels with mitre gears
        RF.setDirection(DcMotor.Direction.FORWARD);
        LB.setDirection(DcMotor.Direction.REVERSE);
        RB.setDirection(DcMotor.Direction.FORWARD);

        //Reverse the arm direction so it moves in the proper direction
        arm.setDirection(DcMotor.Direction.REVERSE);



        // IMU initialization
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.calibrationDataFile = "BNO055IMUCalibration.json"; // see the calibration sample opmode
        parameters.loggingEnabled = true;
        parameters.loggingTag = "IMU";
        parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();
        pidRotate = new PIDController(.003, .00003, 0);


        imu = hardwareMap.get(BNO055IMU.class, "imu");
        imu.initialize(parameters);

        // Set up our telemetry dashboard
        composeTelemetry();  // need to add this method at end of code

        desiredHeading = getHeading();

        moveUtils.initialize(LF, RF, LB, RB, imu, desiredHeading, pidRotate);
        moveUtils.resetEncoders();

        actuatorUtils.initializeActuator(arm, gripper);


        Long startTime = System.currentTimeMillis();
        Long currTime = startTime;

        initOpenCV();
  /*      initTfod();

        if (tfod != null) {
            tfod.activate();

            tfod.setZoom(1.5, 16.0 / 9.0);
        }*/
        actuatorUtils.gripperClose(false);


        waitForStart();
        currTime=System.currentTimeMillis();
        startTime=currTime;
        if (resultROI == 2) {

            // getUpdatedRecognitions() will return null if no new information is available since
            // the last time that call was made.
            done = false;
            while (!done && opModeIsActive()) {
                if (currTime - startTime < 500) {
                    telemetry.addData("Camera: ", "Waiting to make sure valid data is incoming");
                } else {
                    telemetry.addData("Time Delta: ", (currTime - startTime));
                    resultROI = modifyPipeline.getResultROI();
                    if (resultROI == 1) {
                        telemetry.addData("Resulting ROI: ", "Red");
                        done = true;
                    } else if (resultROI == 2) {
                        telemetry.addData("Resulting ROI: ", "Green");
                        done = true;
                    } else if (resultROI == 3) {
                        telemetry.addData("Resulting ROI: ", "Blue");
                        done = true;
                    } else {
                        telemetry.addData("Resulting ROI: ", "Something went wrong.");
                    }
                }
                telemetry.update();
                currTime = System.currentTimeMillis();

            }

        }
        telemetry.update();
        done = false;
        //lift arm up
        actuatorUtils.armPole(4);
        while (((currTime - startTime) < 30000)&& !done && opModeIsActive()) {

            switch (resultROI) {
                case 1:
                    // Far left
                    beginAuto();
                    moveUtils.goStraight(-3,MAX_SPEED,MIN_SPEED,ACCEL);
                    moveUtils.strafeBuddy(24);
                    moveUtils.strafeBuddy(-2);
                    moveUtils.goStraight(2,MIN_SPEED,MIN_SPEED,ACCEL);
                    actuatorUtils.armPole(0);
                    done=true;
                    break;
                case 2:
                    // Middle
                    beginAuto();
                    moveUtils.goStraight(-17,MAX_SPEED,MIN_SPEED,ACCEL);
                    moveUtils.strafeBuddy(24);
                    moveUtils.strafeBuddy(-2);
                    actuatorUtils.armPole(0);
                    done=true;
                    break;
                case 3:
                    // Far right
                    beginAuto();
                    moveUtils.goStraight(-45,MAX_SPEED,MIN_SPEED,ACCEL);
                    moveUtils.strafeBuddy(24);
                    actuatorUtils.armPole(0);
                    done=true;
                    break;
            }

            currTime = System.currentTimeMillis();

        }
    }
    private void beginAuto() throws InterruptedException {
        moveUtils.goStraight(1.5f,MAX_SPEED,MIN_SPEED,ACCEL);
        moveUtils.turnCCW(92); //og 92
        moveUtils.goStraight(18,MAX_SPEED,MIN_SPEED,ACCEL);
        actuatorUtils.gripperOpen(true);

    }

    void composeTelemetry() {

        // At the beginning of each telemetry update, grab a bunch of data
        // from the IMU that we will then display in separate lines.
        telemetry.addAction(new Runnable() {
            @Override
            public void run() {
                // Acquiring the angles is relatively expensive; we don't want
                // to do that in each of the three items that need that info, as that's
                // three times the necessary expense.
                angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
                gravity = imu.getGravity();
            }
        });

        telemetry.addLine()
                .addData("status", new Func<String>() {
                    @Override
                    public String value() {
                        return imu.getSystemStatus().toShortString();
                    }
                })
                .addData("calib", new Func<String>() {
                    @Override
                    public String value() {
                        return imu.getCalibrationStatus().toString();
                    }
                });

        telemetry.addLine()
                .addData("heading", new Func<String>() {
                    @Override
                    public String value() {
                        return formatAngle(angles.angleUnit, angles.firstAngle);
                    }
                })
                .addData("roll", new Func<String>() {
                    @Override
                    public String value() {
                        return formatAngle(angles.angleUnit, angles.secondAngle);
                    }
                })
                .addData("pitch", new Func<String>() {
                    @Override
                    public String value() {
                        return formatAngle(angles.angleUnit, angles.thirdAngle);
                    }
                });

    }

    String formatAngle(AngleUnit angleUnit, double angle) {
        return formatDegrees(AngleUnit.DEGREES.fromUnit(angleUnit, angle));
    }

    String formatDegrees(double degrees) {
        return String.format(Locale.getDefault(), "%.1f", AngleUnit.DEGREES.normalize(degrees));
    }

    public float getHeading() {
        Orientation angles = imu.getAngularOrientation(AxesReference.INTRINSIC,
                AxesOrder.ZYX,
                DEGREES);
        return angles.firstAngle;
    }

    private void initOpenCV() {
        int cameraMonitorViewId2 = hardwareMap.appContext.getResources().getIdentifier(
                "cameraMonitorViewId",
                "id",
                hardwareMap.appContext.getPackageName());
        // For a webcam (uncomment below)
        webCam = OpenCvCameraFactory.getInstance().createWebcam(hardwareMap.get(WebcamName.class, "Webcam 1"), cameraMonitorViewId2);
        // For a phone camera (uncomment below)
        // webCam = OpenCvCameraFactory.getInstance().createInternalCamera(OpenCvInternalCamera.CameraDirection.BACK, cameraMonitorViewId2);
        webCam.setPipeline(modifyPipeline);
        webCam.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener() {
            @Override
            public void onOpened() {
                webCam.startStreaming(320,240, OpenCvCameraRotation.UPRIGHT);
                telemetry.addData("Pipeline: ", "Initialized");
                telemetry.update();
                isCameraStreaming = true;
            }

            @Override
            public void onError(int errorCode) {
                telemetry.addData("Error: ", "Something went wrong :(");
                telemetry.update();
            }
        });
    }
    /*private void initTfod() {
        int tfodMonitorViewId = hardwareMap.appContext.getResources().getIdentifier(
                "tfodMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        TFObjectDetector.Parameters tfodParameters = new TFObjectDetector.Parameters(tfodMonitorViewId);
        tfodParameters.minResultConfidence = 0.75f;
        tfodParameters.isModelTensorFlow2 = true;
        tfodParameters.inputSize = 300;
        tfod = ClassFactory.getInstance().createTFObjectDetector(tfodParameters, vuforia);

        // Use loadModelFromAsset() if the TF Model is built in as an asset by Android Studio
        // Use loadModelFromFile() if you have downloaded a custom team model to the Robot Controller's FLASH.
        tfod.loadModelFromAsset(TFOD_MODEL_ASSET, LABELS);
        // tfod.loadModelFromFile(TFOD_MODEL_FILE, LABELS);
    }
*/
}
