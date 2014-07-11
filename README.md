Notifyr Android Demo
====================

This repository holds the Android application code for Praetzel's demo in 4A. The app simply has a textbox and a send button. When the button is pressed, the text from the text box will be sent and displayed on the Notifyr. The app will need to discover devices first, but then will have to remeber the Notifyr device to automatically connect after initial setup.

This application demos our ability to take input, send it to the device, and display it properly.

## Pushing new code (for those new to git) ##

To push new code, we will be using pull requests. To illustrate how this works consider the following example. Say that you want to add a new feature to the project. The first step is to pull the latest master to your local repository. You can do this by running while you're in your project directory:

`git pull`

The next thing is to make sure that you are currently working from the `master` branch, as that's what we wish to base our modifications on. You can do this with the following command:

`git checkout master`

Once you're in master, you're gonna make a new branch. All of your changes will be in this branch, which makes it easy to merge later once other's have reviewed your code. To make a new branch, run the following command:

`git checkout -b your_branch_name`

`your_branch_name` is whatever you wanna call it. You can call it anything (yes, Andrew, you can call it "Hindi_McBindi") since it'll get deleted later anyway. At this point, you can make your changes. When you're done, you gotta push your changes to Github so others can see and review it. Do this with the following command:

`git push origin your_branch_name`

`your_branch_name` should match the name that you entered before, although I don't think it matters too much. Now the code is on Github. If you go on Github into the repository, you'll see a "Create Pull Request" button. Just click that and it'll make a pull request for your branch.

Everyone in the project will be able to review and when we're ready, we simply click merge and your code will be merged into the `master` branch.

## Code Style ##

Right now, we don't have any automated tools to enforce code style, so here an example of the code style we should use:

```java
public class ClassName { // upper camel case for class names
  public static void thisIsAFunction() { // lower camel case for method names
    int this_is_a_variable; // underscores for variable names
  }
  
  public static void thisFunctionHasManyParameters(int x, // multiline parameters for long method signatures
    int y,
    String z) {
    ...
  }
}
