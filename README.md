Permission-Redelegation
=======================

An open source tool that detects permission redelegation vulnerabilities of an android app.
For more info look at this paper:
https://www.usenix.org/legacy/event/sec11/tech/full_papers/Felt.pdf

What is permission redelegation? 
Permission re-delegation occurs when an application with a permission performs a privileged task on behalf
of an application without that permission. This is a confused deputy attack or privilege escalation attack.

Objective: Build an open source tool that detects permission redelegation vulnerabilities of an android app. 

In what ways is this different from the usenix paper:
It's an open source code available to developers. This is especially important for two reasons.
  1.Since the permission mapping changes in different APIs, it is important to have an open source tool 
    that its developers can update the permission maps when a new version comes out. 
  2.Application Developers can get better accuracy through static analysis by having access 
    to their source code and class files. 
The original paper does not look at permission redelegation resulting from sending and receiving intents. 
This requires Data flow analysis. It also does not look at as permission redelegation resulting from usage 
of content providers. 
The paper has a bottom up approach Searching the call graphs for finding restricted API. 
This approach is naive because of two reasons:
  1.It will be significantly slower than a top to bottom approach. 
  2.The call graph analysis does not handle inheritance relationships
  Instead I'm proposing a top to bottom approach that handles inheritance relationships as well.
The usenix paper ignores any component that is protected by a dangerous permission. This means they ignore
a component that is redelegating two permissions, but is only protected by one permission. I'm proposing 
to consider even the components that are protected by a permission, unless they are protected by a signature permission.
